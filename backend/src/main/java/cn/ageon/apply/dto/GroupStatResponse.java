package cn.ageon.apply.dto;

public record GroupStatResponse(String key, int total, int offers, int rejected, double responseRate) {
}