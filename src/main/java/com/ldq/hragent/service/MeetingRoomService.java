package com.ldq.hragent.service;

import com.ldq.hragent.model.hr.MeetingRoomBookingResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MeetingRoomService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public MeetingRoomService(@Qualifier("bizJdbcTemplate") JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public MeetingRoomBookingResult bookRoom(String roomName, String startTime, String endTime, String purpose) {
        permissionService.requireEmployeeOrAbove();
        Long userId = permissionService.currentUserId();

        if (roomName == null || roomName.isBlank()) {
            return new MeetingRoomBookingResult(null, null, startTime, endTime, "REJECTED", "会议室名称不能为空");
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startTime);
            end = LocalDateTime.parse(endTime);
        } catch (Exception e) {
            return new MeetingRoomBookingResult(null, roomName, startTime, endTime, "REJECTED", "时间格式不正确，请使用 ISO-8601");
        }

        if (!end.isAfter(start)) {
            return new MeetingRoomBookingResult(null, roomName, startTime, endTime, "REJECTED", "结束时间必须晚于开始时间");
        }

        Integer conflictCount = jdbcTemplate.query("""
                SELECT COUNT(1)
                FROM meeting_room_booking
                WHERE room_name = ?
                  AND status IN ('BOOKED', 'PENDING')
                  AND start_time < ?
                  AND end_time > ?
                """, rs -> rs.next() ? rs.getInt(1) : 0, roomName, end, start);

        if (conflictCount != null && conflictCount > 0) {
            return new MeetingRoomBookingResult(null, roomName, startTime, endTime, "CONFLICT", "该会议室在此时间段已被占用");
        }

        jdbcTemplate.update("""
                INSERT INTO meeting_room_booking
                (user_id, room_name, start_time, end_time, purpose, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'BOOKED', NOW(), NOW())
                """,
                userId,
                roomName,
                start,
                end,
                purpose
        );

        Long bookingId = jdbcTemplate.query("""
                SELECT id
                FROM meeting_room_booking
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, userId);

        return new MeetingRoomBookingResult(
                bookingId,
                roomName,
                start.toString(),
                end.toString(),
                "BOOKED",
                "会议室预约成功。后续可接入企业日历与门禁联动。"
        );
    }
}