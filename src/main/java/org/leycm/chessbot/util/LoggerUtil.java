package org.leycm.chessbot.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LoggerUtil {

    public static String formatMessage(String msg, Object... args) {
        if (args == null || args.length == 0) {
            return msg;
        }

        Throwable throwable = null;
        if (args[args.length - 1] instanceof Throwable) {
            throwable = (Throwable) args[args.length - 1];
            args = removeLastElement(args);
        }

        String formattedMsg = msg;
        for (Object arg : args) {
            String argString = arg != null ? arg.toString() : "null";
            formattedMsg = formattedMsg.replaceFirst("\\{}", argString);
        }

        if (throwable != null) {
            formattedMsg += "\n" + getStackTraceAsString(throwable);
        }

        return formattedMsg;
    }

    private static Object @NotNull [] removeLastElement(Object @NotNull [] array) {
        Object[] newArray = new Object[array.length - 1];

        System.arraycopy(
                array,
                0,
                newArray,
                0,
                newArray.length
        );

        return newArray;
    }

    private static @NotNull String getStackTraceAsString(@NotNull Throwable throwable) {
        StringBuilder sb = new StringBuilder();

        sb.append(throwable).append("\n");

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\t").append(element.toString()).append("\n");
        }

        return sb.toString();
    }
}
