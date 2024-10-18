package com.github.im.common.model;


/**
 * 用户信息上下文
 */
public class AccountContext {
    private static final ThreadLocal<AccountInfo> currentLocal = new ThreadLocal<>();

    public static void rmAccount(){
        currentLocal.remove();
    }
    public static void setCurAccount(AccountInfo curAccount){
        currentLocal.set(curAccount);
    }
    public static AccountInfo getCurAccount(){
        return currentLocal.get();
    }


}
