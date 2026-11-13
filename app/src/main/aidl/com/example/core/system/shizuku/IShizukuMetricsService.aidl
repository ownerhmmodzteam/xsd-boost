package com.example.core.system.shizuku;

interface IShizukuMetricsService {
    void destroy();
    float readCpuUsagePercent();
    float readThermalCelsius();
    String executeShellCommand(String command);
}
