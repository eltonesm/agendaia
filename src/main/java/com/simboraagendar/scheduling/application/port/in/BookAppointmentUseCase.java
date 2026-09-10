package com.simboraagendar.scheduling.application.port.in;

public interface BookAppointmentUseCase {

    BookedAppointment handle(BookAppointmentCommand command);
}
