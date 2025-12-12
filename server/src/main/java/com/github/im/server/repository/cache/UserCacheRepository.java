//package com.github.im.server.repository.cache;
//
//import com.github.im.dto.user.UserInfo;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.keyvalue.repository.KeyValueRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public class UserCacheRepository implements KeyValueRepository<UserInfo, Long> {
//
//
//    @Override
//    public <S extends UserInfo> S save(S entity) {
//        return null;
//    }
//
//    @Override
//    public <S extends UserInfo> List<S> saveAll(Iterable<S> entities) {
//        return List.of();
//    }
//
//    @Override
//    public Optional<UserInfo> findById(Long aLong) {
//        return Optional.empty();
//    }
//
//    @Override
//    public boolean existsById(Long aLong) {
//        return false;
//    }
//
//    @Override
//    public List<UserInfo> findAll() {
//        return List.of();
//    }
//
//    @Override
//    public List<UserInfo> findAllById(Iterable<Long> longs) {
//        return List.of();
//    }
//
//    @Override
//    public long count() {
//        return 0;
//    }
//
//    @Override
//    public void deleteById(Long aLong) {
//
//    }
//
//    @Override
//    public void delete(UserInfo entity) {
//
//    }
//
//    @Override
//    public void deleteAllById(Iterable<? extends Long> longs) {
//
//    }
//
//    @Override
//    public void deleteAll(Iterable<? extends UserInfo> entities) {
//
//    }
//
//    @Override
//    public void deleteAll() {
//
//    }
//
//    @Override
//    public List<UserInfo> findAll(Sort sort) {
//        return List.of();
//    }
//
//    @Override
//    public Page<UserInfo> findAll(Pageable pageable) {
//        return null;
//    }
//}
