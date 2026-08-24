package com.github.im.server.workbench.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.dto.workbench.approval.*;
import com.github.im.server.workbench.approval.model.*;
import com.github.im.server.workbench.approval.repository.*;
import com.github.im.server.workbench.common.audit.WorkbenchAuditService;
import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.error.*;
import com.github.im.server.workbench.common.integration.OrganizationAdapter;
import com.github.im.server.workbench.common.permission.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ApprovalService {
    private final WorkbenchPermissionService permissions;
    private final OrganizationAdapter organization;
    private final ApprovalDefinitionRepository definitions;
    private final ApprovalInstanceRepository instances;
    private final ApprovalNodeRepository nodes;
    private final ApprovalActionRepository actions;
    private final ApprovalCcRepository cc;
    private final ApprovalStateMachine stateMachine;
    private final WorkbenchAuditService audit;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApprovalService(WorkbenchPermissionService permissions, OrganizationAdapter organization,
            ApprovalDefinitionRepository definitions, ApprovalInstanceRepository instances,
            ApprovalNodeRepository nodes, ApprovalActionRepository actions, ApprovalCcRepository cc,
            ApprovalStateMachine stateMachine, WorkbenchAuditService audit, ObjectMapper objectMapper,
            Clock workbenchClock) {
        this.permissions=permissions; this.organization=organization; this.definitions=definitions;
        this.instances=instances; this.nodes=nodes; this.actions=actions; this.cc=cc;
        this.stateMachine=stateMachine; this.audit=audit; this.objectMapper=objectMapper; this.clock=workbenchClock;
    }

    @Transactional(readOnly=true)
    public List<ApprovalDefinitionDTO> definitions() {
        permissions.require(WorkbenchPermission.VIEW_WORKBENCH);
        return definitions.findByEnabledTrueOrderByNameAsc().stream().map(this::toDefinition).toList();
    }

    @Transactional
    public ApprovalInstanceDTO create(CreateApprovalRequest request) {
        CurrentWorkContext context=permissions.require(WorkbenchPermission.APPROVAL_CREATE);
        if (request==null || request.definitionId()==null || request.approverIds()==null || request.approverIds().isEmpty())
            bad("definitionId 和 approverIds 不能为空");
        ApprovalDefinition definition=definitions.findById(request.definitionId())
                .filter(value->Boolean.TRUE.equals(value.getEnabled()))
                .orElseThrow(()->WorkbenchException.badRequest(WorkbenchErrorCode.APPROVAL_INVALID_REQUEST,"审批定义不存在或已停用"));
        String title=normalize(request.title(),200,"审批标题");
        String form=validJson(request.formDataJson());
        List<Long> approvers=distinct(request.approverIds());
        for(Long userId:approvers) organization.requireActiveMember(context.companyId(),userId);
        List<Long> copies=distinct(request.ccUserIds());
        for(Long userId:copies) organization.requireActiveMember(context.companyId(),userId);

        ApprovalInstance instance=instances.save(ApprovalInstance.builder()
                .definitionId(definition.getDefinitionId()).definitionVersion(definition.getDefinitionVersion())
                .title(title).applicantId(context.userId()).departmentId(request.departmentId())
                .status(ApprovalStatus.DRAFT).formDataJson(form).build());
        for(int i=0;i<approvers.size();i++) nodes.save(ApprovalNode.builder().instanceId(instance.getInstanceId())
                .nodeOrder(i+1).assigneeId(approvers.get(i)).status(ApprovalNodeStatus.WAITING).build());
        for(Long userId:copies) cc.save(ApprovalCc.builder().instanceId(instance.getInstanceId()).userId(userId).build());
        return detailInternal(instance,context.userId());
    }

    @Transactional(readOnly=true)
    public ApprovalInstanceDTO detail(Long id) {
        CurrentWorkContext context=permissions.require(WorkbenchPermission.VIEW_WORKBENCH);
        ApprovalInstance instance=requireInstance(id);
        requireView(instance,context.userId());
        return detailInternal(instance,context.userId());
    }

    @Transactional(readOnly=true)
    public List<ApprovalSummaryDTO> list(String view,int requestedLimit) {
        CurrentWorkContext context=permissions.require(WorkbenchPermission.VIEW_WORKBENCH);
        int limit=Math.max(1,Math.min(requestedLimit,100));
        List<ApprovalInstance> result=switch(view==null?"MY_SUBMITTED":view.toUpperCase(Locale.ROOT)) {
            case "MY_SUBMITTED" -> instances.findByApplicantIdOrderByCreatedAtDesc(context.userId(),PageRequest.of(0,limit));
            case "PENDING_FOR_ME" -> loadDistinct(nodes.findByAssigneeIdAndStatusOrderByStartedAtDesc(context.userId(),ApprovalNodeStatus.PENDING).stream().map(ApprovalNode::getInstanceId).toList(),limit);
            case "PROCESSED_BY_ME" -> loadDistinct(actions.findByOperatorIdOrderByCreatedAtDesc(context.userId()).stream().map(ApprovalAction::getInstanceId).toList(),limit);
            case "CC_TO_ME" -> loadDistinct(cc.findByUserIdOrderByCreatedAtDesc(context.userId()).stream().map(ApprovalCc::getInstanceId).toList(),limit);
            default -> throw WorkbenchException.badRequest(WorkbenchErrorCode.APPROVAL_INVALID_REQUEST,"不支持的审批视图: "+view);
        };
        return result.stream().map(this::toSummary).toList();
    }

    @Transactional
    public ApprovalInstanceDTO act(Long id, ApprovalActionType action, ApprovalActionRequest request) {
        CurrentWorkContext context=permissions.require(action==ApprovalActionType.SUBMIT || action==ApprovalActionType.RESUBMIT || action==ApprovalActionType.CANCEL
                ? WorkbenchPermission.APPROVAL_CREATE : WorkbenchPermission.APPROVAL_ACT);
        try {
            ApprovalInstance instance=requireInstance(id);
            stateMachine.require(instance.getStatus(),action);
            Long actionNodeId=instance.getCurrentNodeOrder()==null?null:
                    nodes.findByInstanceIdAndNodeOrder(instance.getInstanceId(),instance.getCurrentNodeOrder())
                            .map(ApprovalNode::getNodeId).orElse(null);
            if(action==ApprovalActionType.SUBMIT || action==ApprovalActionType.RESUBMIT || action==ApprovalActionType.CANCEL) {
                if(!Objects.equals(instance.getApplicantId(),context.userId())) deny();
            } else {
                ApprovalNode current=requireCurrentNode(instance);
                if(!Objects.equals(current.getAssigneeId(),context.userId())) deny();
            }
            apply(instance,action,request,context);
            instance=instances.saveAndFlush(instance);
            if(actionNodeId==null && instance.getCurrentNodeOrder()!=null) actionNodeId=requireCurrentNode(instance).getNodeId();
            record(instance,actionNodeId,action,context.userId(),request==null?null:request.comment());
            audit.record(context.tenantScope(),context.userId(),"APPROVAL",action.name(),"APPROVAL",
                    String.valueOf(instance.getInstanceId()),null,instance.getStatus().name(),Map.of());
            return detailInternal(instance,context.userId());
        } catch(OptimisticLockingFailureException exception) {
            throw WorkbenchException.conflict(WorkbenchErrorCode.APPROVAL_CONFLICT,"审批已被其他请求处理，请刷新后重试");
        }
    }

    private void apply(ApprovalInstance instance,ApprovalActionType action,ApprovalActionRequest request,CurrentWorkContext context) {
        LocalDateTime now=LocalDateTime.now(clock);
        switch(action) {
            case SUBMIT -> activate(instance,1,now);
            case RESUBMIT -> {
                if(request!=null && request.formDataJson()!=null) instance.setFormDataJson(validJson(request.formDataJson()));
                activate(instance,Objects.requireNonNull(instance.getCurrentNodeOrder()),now);
            }
            case APPROVE -> {
                ApprovalNode node=requireCurrentNode(instance); finish(node,ApprovalNodeStatus.APPROVED,now);
                Optional<ApprovalNode> next=nodes.findByInstanceIdAndNodeOrder(instance.getInstanceId(),node.getNodeOrder()+1);
                if(next.isPresent()) activate(instance,next.get().getNodeOrder(),now);
                else { instance.setStatus(ApprovalStatus.APPROVED); instance.setCurrentNodeOrder(null); instance.setCompletedAt(now); }
            }
            case REJECT -> terminal(instance,ApprovalStatus.REJECTED,ApprovalNodeStatus.REJECTED,now);
            case RETURN -> { terminal(instance,ApprovalStatus.RETURNED,ApprovalNodeStatus.RETURNED,now); instance.setCompletedAt(null); }
            case CANCEL -> {
                if(instance.getStatus()==ApprovalStatus.PENDING) finish(requireCurrentNode(instance),ApprovalNodeStatus.CANCELLED,now);
                instance.setStatus(ApprovalStatus.CANCELLED); instance.setCurrentNodeOrder(null); instance.setCompletedAt(now);
            }
        }
    }

    private void activate(ApprovalInstance instance,int order,LocalDateTime now) {
        ApprovalNode node=nodes.findByInstanceIdAndNodeOrder(instance.getInstanceId(),order)
                .orElseThrow(()->WorkbenchException.conflict(WorkbenchErrorCode.APPROVAL_INVALID_TRANSITION,"审批节点不存在"));
        node.setStatus(ApprovalNodeStatus.PENDING); node.setStartedAt(now); node.setCompletedAt(null); nodes.save(node);
        instance.setStatus(ApprovalStatus.PENDING); instance.setCurrentNodeOrder(order);
        if(instance.getSubmittedAt()==null) instance.setSubmittedAt(now);
    }
    private void terminal(ApprovalInstance instance,ApprovalStatus status,ApprovalNodeStatus nodeStatus,LocalDateTime now) {
        finish(requireCurrentNode(instance),nodeStatus,now); instance.setStatus(status); instance.setCompletedAt(now);
    }
    private void finish(ApprovalNode node,ApprovalNodeStatus status,LocalDateTime now) { node.setStatus(status); node.setCompletedAt(now); nodes.saveAndFlush(node); }
    private ApprovalNode requireCurrentNode(ApprovalInstance value) { return nodes.findByInstanceIdAndNodeOrder(value.getInstanceId(),value.getCurrentNodeOrder()).orElseThrow(()->WorkbenchException.conflict(WorkbenchErrorCode.APPROVAL_CONFLICT,"当前审批节点已变化")); }
    private void record(ApprovalInstance instance,Long nodeId,ApprovalActionType type,Long operator,String comment) { actions.save(ApprovalAction.builder().instanceId(instance.getInstanceId()).nodeId(nodeId).operatorId(operator).action(type).comment(normalizeOptional(comment,1000)).build()); }
    private ApprovalInstance requireInstance(Long id) { return instances.findById(id).orElseThrow(()->WorkbenchException.notFound(WorkbenchErrorCode.APPROVAL_NOT_FOUND,"审批不存在: "+id)); }
    private void requireView(ApprovalInstance value,Long userId) { if(!Objects.equals(value.getApplicantId(),userId) && !nodes.existsByInstanceIdAndAssigneeId(value.getInstanceId(),userId) && !cc.existsByInstanceIdAndUserId(value.getInstanceId(),userId) && !actions.existsByInstanceIdAndOperatorId(value.getInstanceId(),userId)) deny(); }
    private void deny() { throw WorkbenchException.forbidden(WorkbenchErrorCode.APPROVAL_ACCESS_DENIED,"无权查看或处理该审批"); }
    private List<ApprovalInstance> loadDistinct(List<Long> ids,int limit) { return ids.stream().distinct().limit(limit).map(instances::findById).flatMap(Optional::stream).toList(); }
    private List<Long> distinct(List<Long> values) { return values==null?List.of():values.stream().filter(Objects::nonNull).distinct().toList(); }
    private String validJson(String value) { String json=value==null||value.isBlank()?"{}":value.trim(); try { if(!objectMapper.readTree(json).isObject()) bad("formDataJson 必须是 JSON object"); return json; } catch(Exception e) { bad("formDataJson 不是合法 JSON"); return json; } }
    private String normalize(String value,int max,String field) { if(value==null||value.isBlank()) bad(field+"不能为空"); String result=value.trim(); if(result.length()>max) bad(field+"不能超过 "+max+" 个字符"); return result; }
    private String normalizeOptional(String value,int max) { if(value==null||value.isBlank()) return null; String result=value.trim(); if(result.length()>max) bad("comment 不能超过 "+max+" 个字符"); return result; }
    private void bad(String message) { throw WorkbenchException.badRequest(WorkbenchErrorCode.APPROVAL_INVALID_REQUEST,message); }
    private ApprovalDefinitionDTO toDefinition(ApprovalDefinition d) { return new ApprovalDefinitionDTO(d.getDefinitionId(),d.getCode(),d.getName(),d.getFormSchemaJson(),d.getDefinitionVersion()); }
    private ApprovalSummaryDTO toSummary(ApprovalInstance i) { return new ApprovalSummaryDTO(i.getInstanceId(),i.getTitle(),i.getApplicantId(),i.getStatus().name(),i.getCurrentNodeOrder(),i.getSubmittedAt(),i.getUpdatedAt()); }
    private ApprovalInstanceDTO detailInternal(ApprovalInstance i,Long userId) { List<ApprovalNodeDTO> ns=nodes.findByInstanceIdOrderByNodeOrderAsc(i.getInstanceId()).stream().map(n->new ApprovalNodeDTO(n.getNodeId(),n.getNodeOrder(),n.getAssigneeId(),n.getStatus().name(),n.getStartedAt(),n.getCompletedAt())).toList(); List<ApprovalActionDTO> as=actions.findByInstanceIdOrderByCreatedAtAscActionIdAsc(i.getInstanceId()).stream().map(a->new ApprovalActionDTO(a.getActionId(),a.getNodeId(),a.getOperatorId(),a.getAction().name(),a.getComment(),a.getCreatedAt())).toList(); List<Long> copies=cc.findByInstanceIdOrderByCreatedAtAsc(i.getInstanceId()).stream().map(ApprovalCc::getUserId).toList(); return new ApprovalInstanceDTO(i.getInstanceId(),i.getDefinitionId(),i.getDefinitionVersion(),i.getTitle(),i.getApplicantId(),i.getDepartmentId(),i.getStatus().name(),i.getFormDataJson(),i.getCurrentNodeOrder(),i.getVersion()==null?0:i.getVersion(),i.getSubmittedAt(),i.getCompletedAt(),i.getCreatedAt(),i.getUpdatedAt(),ns,as,copies,available(i,userId)); }
    private List<String> available(ApprovalInstance i,Long userId) { if(Objects.equals(i.getApplicantId(),userId)) return switch(i.getStatus()){case DRAFT->List.of("SUBMIT","CANCEL");case RETURNED->List.of("RESUBMIT","CANCEL");case PENDING->List.of("CANCEL");default->List.of();}; if(i.getStatus()==ApprovalStatus.PENDING && requireCurrentNode(i).getAssigneeId().equals(userId)) return List.of("APPROVE","REJECT","RETURN"); return List.of(); }
}
