package com.github.im.server.security;

import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("companyOwnershipChecker")
public class CompanyOwnershipChecker {

    @Autowired
    private UserRepository userRepository;

    /**
     * 检查用户是否属于指定公司
     *
     * @param companyId 公司ID
     * @param user 用户对象
     * @return 如果用户属于该公司则返回true，否则返回false
     */
    public boolean userBelongsToCompany(Long companyId, User user) {
        if (user.getCompanyId() == null) {
            return false;
        }
        return user.getCompanyId().equals(companyId);
    }

    /**
     * 检查用户是否属于指定公司
     *
     * @param userId    用户ID
     * @param companyId 公司ID
     * @return 如果用户属于该公司则返回true，否则返回false
     */
    public boolean userBelongsToCompany(Long userId, Long companyId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.getCompanyId() != null && user.getCompanyId().equals(companyId);
        }
        return false;
    }

    /**
     * 检查两个用户是否属于同一家公司
     *
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 如果两个用户属于同一家公司则返回true，否则返回false
     */
    public boolean usersBelongToSameCompany(Long userId1, Long userId2) {
        Optional<User> user1Opt = userRepository.findById(userId1);
        Optional<User> user2Opt = userRepository.findById(userId2);

        if (user1Opt.isPresent() && user2Opt.isPresent()) {
            User user1 = user1Opt.get();
            User user2 = user2Opt.get();
            return user1.getCompanyId() != null && user1.getCompanyId().equals(user2.getCompanyId());
        }
        return false;
    }
}