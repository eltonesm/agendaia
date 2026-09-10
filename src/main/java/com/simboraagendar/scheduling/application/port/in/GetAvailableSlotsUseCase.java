package com.simboraagendar.scheduling.application.port.in;

import com.simboraagendar.scheduling.domain.AvailableSlot;
import java.util.List;

public interface GetAvailableSlotsUseCase {

    List<AvailableSlot> handle(GetAvailableSlotsQuery query);
}
