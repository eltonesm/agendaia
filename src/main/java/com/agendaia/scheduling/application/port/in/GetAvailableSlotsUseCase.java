package com.agendaia.scheduling.application.port.in;

import com.agendaia.scheduling.domain.AvailableSlot;
import java.util.List;

public interface GetAvailableSlotsUseCase {

    List<AvailableSlot> handle(GetAvailableSlotsQuery query);
}
