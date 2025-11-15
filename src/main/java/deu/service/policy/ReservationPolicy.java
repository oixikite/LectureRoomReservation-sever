package deu.service.policy;

import deu.model.dto.request.data.reservation.RoomReservationRequest;

public interface ReservationPolicy {
    void validate(RoomReservationRequest payload) throws Exception;
}
