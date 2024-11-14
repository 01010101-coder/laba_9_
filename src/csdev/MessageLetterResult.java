package csdev;

import java.io.Serializable;

public class MessageLetterResult extends MessageResult implements Serializable {

    private static final long serialVersionUID = 1L;
    private String message; // Поле для хранения текста сообщения

    // Конструктор с сообщением
    public MessageLetterResult(String message) {
        super(Protocol.CMD_LETTER);
        this.message = message;
    }

    // Конструктор для сообщений об ошибке
    public MessageLetterResult(String errorMessage, boolean isError) {
        super(Protocol.CMD_LETTER, errorMessage);
    }

    // Метод для получения текста сообщения
    public String getMessage() {
        return message;
    }
}
