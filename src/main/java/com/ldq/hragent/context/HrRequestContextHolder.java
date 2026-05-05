package com.ldq.hragent.context;

public final class HrRequestContextHolder {

    private static final ThreadLocal<HrRequestContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private HrRequestContextHolder() {
    }

    public static void setContext(HrRequestContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static HrRequestContext getContext() {
        HrRequestContext context = CONTEXT_HOLDER.get();
        if (context == null) {
            throw new IllegalStateException("当前请求上下文不存在");
        }
        return context;
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}