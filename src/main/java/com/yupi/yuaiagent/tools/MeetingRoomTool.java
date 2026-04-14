package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.model.hr.MeetingRoomBookingResult;
import com.yupi.yuaiagent.service.MeetingRoomService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class MeetingRoomTool {

    private final MeetingRoomService meetingRoomService;

    public MeetingRoomTool(MeetingRoomService meetingRoomService) {
        this.meetingRoomService = meetingRoomService;
    }

    @Tool(description = "为当前登录员工预约会议室。startTime 和 endTime 使用 ISO-8601 日期时间格式")
    public MeetingRoomBookingResult bookRoom(String roomName, String startTime, String endTime, String purpose) {
        return meetingRoomService.bookRoom(roomName, startTime, endTime, purpose);
    }
}