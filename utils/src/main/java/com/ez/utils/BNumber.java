package com.ez.utils;

import java.math.BigDecimal;

public class BNumber {

    //四舍五入
    public static BigDecimal round(double number, int decimal) {

        return new BigDecimal(number).setScale(decimal, BigDecimal.ROUND_HALF_UP);

    }
}
