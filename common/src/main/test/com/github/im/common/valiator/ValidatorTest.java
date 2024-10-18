package com.github.im.common.valiator;

import cn.hutool.core.collection.CollectionUtil;
import com.github.im.common.model.AccountInfo;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * @author pengpeng
 * @description
 * @date 2023/1/11
 */
@Slf4j
public class ValidatorTest {

    @Test
    public void validator(){
        AccountInfo accountInfo = new AccountInfo();

//        Set<ConstraintViolation<AccountInfo>> constraintViolations = ValidatorUtil.validateOne(accountInfo);

//        Assertions.assertTrue(CollectionUtil.isNotEmpty(constraintViolations));

    }



}
