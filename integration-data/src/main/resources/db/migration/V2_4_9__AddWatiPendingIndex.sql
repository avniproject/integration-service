CREATE INDEX idx_wati_msg_req_pending_lookup
    ON wati_message_request (integration_system_id, status, next_retry_time);
