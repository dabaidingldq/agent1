package com.ldq.hragent.tools;

import com.ldq.hragent.model.hr.MeetingRoomBookingResult;
import com.ldq.hragent.service.MeetingRoomService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class MeetingRoomTool {

    private final MeetingRoomService meetingRoomService;

    public MeetingRoomTool(MeetingRoomService meetingRoomService) {
        this.meetingRoomService = meetingRoomService;
    }

    @Tool(description = """
            为当前登录员工预约会议室。
            适用场景：
            - 用户说“帮我预约会议室”
            - 用户说“明天下午两点订会议室”
            - 用户通过快捷入口提交会议室预约信息
            
            参数规则：
            - roomName：会议室名称。如果用户没指定会议室，可传“未指定”或先追问，取决于业务要求
            - startTime：开始时间，尽量转换为 ISO-8601 日期时间格式，例如 2026-04-18T14:00:00
            - endTime：结束时间，尽量转换为 ISO-8601 日期时间格式，例如 2026-04-18T15:30:00
            - purpose：会议用途或主题
            - 如果缺少开始时间或结束时间，不要调用工具，应先追问
            - 工具返回后，应说明预约是否成功、会议室、时间和后续注意事项
            """)
    public MeetingRoomBookingResult bookRoom(String roomName, String startTime, String endTime, String purpose) {
        return meetingRoomService.bookRoom(roomName, startTime, endTime, purpose);
    }
}