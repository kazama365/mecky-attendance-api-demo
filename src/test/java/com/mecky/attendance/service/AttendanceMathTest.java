package com.mecky.attendance.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class AttendanceMathTest {
 @Test void gracePeriodDoesNotMarkOnTimeEmployeeLate(){LocalTime expected=LocalTime.of(9,0);assertEquals(0,Math.max(0,(int)Duration.between(expected.plusMinutes(5),LocalTime.of(9,5)).toMinutes()));}
 @Test void minutesAfterGraceBecomeLate(){LocalTime expected=LocalTime.of(9,0);assertEquals(3,Math.max(0,(int)Duration.between(expected.plusMinutes(5),LocalTime.of(9,8)).toMinutes()));}
}
