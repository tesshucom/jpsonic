package com.tesshu.jpsonic.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class MediaScannerServiceUtilsTestUtils {
    
    private MediaScannerServiceUtilsTestUtils() {
    }

    static void invokeUtils(MediaScannerServiceUtils utils, String methodName) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Method method = utils.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(utils, new Object[0]);
    }

}
