package com.mecky.attendance.service;
import org.junit.jupiter.api.Test;
import java.math.*;
import static org.junit.jupiter.api.Assertions.*;
class PayrollFormulaTest {
 @Test void dailyRateTimesPresentDaysUsesDecimalRounding(){BigDecimal daily=new BigDecimal("525.00");BigDecimal gross=daily.multiply(BigDecimal.valueOf(21)).setScale(2,RoundingMode.HALF_UP);assertEquals(new BigDecimal("11025.00"),gross);}
}
