package com.ljn;

public class Emoji {
    public static String Status(String status){
        switch (status.toLowerCase()){
            case "test":
                return "\uD83D\uDD36";
            case "open":
                return "⚪";
            case "in progress":
                return "\uD83D\uDD35";
            case "closed":
                return "\uD83C\uDFC1";
            case "coding":
                return "\uD83D\uDD35";
            case "code review":
                return "\uD83D\uDD36";
                default:
                    return status;
        }
    }

    public static String Type(String type){
        switch (type.toLowerCase()){
            case "bug":
                return "\uD83D\uDC1E";
            case "enhancement":
                return "\uD83C\uDF31";
            case "requirement/user story":
                return "☘";
            case "task":
                return "⚡";
            case "bug sub-task":
                return "\uD83D\uDC1B";
            default:
                return type;
        }
    }
}
