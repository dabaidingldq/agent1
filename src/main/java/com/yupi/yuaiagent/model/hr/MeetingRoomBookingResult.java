package com.yupi.yuaiagent.model.hr;

public record MeetingRoomBookingResult(
        Long bookingId,
        String roomName,
        String startTime,
        String endTime,
        String status,
        String message
) {
}