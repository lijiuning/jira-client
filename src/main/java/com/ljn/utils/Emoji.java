package com.ljn.utils;

public class Emoji {
    public static boolean iconMode = false;
    public static String Status(String status){
        if(!iconMode)
            return "";

        switch (status.toLowerCase()){
            case "test":
            case "review":
                return "\uD83D\uDD36";
            case "open":
                return "⚪";
            case "in progress":
            case "design":
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
        if(!iconMode)
        {
            switch (type.toLowerCase()){
                case "bug":
                    return "Bug.";
                case "enhancement":
                    return "Enh.";
                case "requirement/user story":
                    return "Fte.";
                case "task":
                    return "Tsk.";
                case "bug sub-task":
                    return "Isd.";
                default:
                    return type;
            }
        }

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
