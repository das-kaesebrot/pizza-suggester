package eu.kaesebrot.dev.handler;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class UpdateHandler implements IUpdateHandler {
    @Override
    public void handleUpdate(Update update) {

    }
}
