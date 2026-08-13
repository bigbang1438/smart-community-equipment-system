package com.smart.community.controller;

import com.smart.community.common.PageResult;
import com.smart.community.common.Result;
import com.smart.community.common.RoleRequired;
import com.smart.community.entity.Device;
import com.smart.community.service.DeviceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/page")
    public Result<PageResult<Device>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(required = false) String status) {
        return Result.ok(deviceService.page(page, size, keyword, type, status));
    }

    @GetMapping("/list")
    public Result<List<Device>> list() {
        return Result.ok(deviceService.list());
    }

    @GetMapping("/{id}")
    public Result<Device> detail(@PathVariable Long id) {
        return Result.ok(deviceService.getById(id));
    }

    @GetMapping("/byCode")
    public Result<Device> byCode(@RequestParam String code) {
        return Result.ok(deviceService.getByCode(code));
    }

    @PostMapping
    @RoleRequired({"ADMIN", "MAINTAINER"})
    public Result<Device> add(@RequestBody Device device) {
        return Result.ok(deviceService.add(device));
    }

    @PutMapping("/{id}")
    @RoleRequired({"ADMIN", "MAINTAINER"})
    public Result<Device> update(@PathVariable Long id, @RequestBody Device device) {
        device.setId(id);
        return Result.ok(deviceService.updateDevice(device));
    }

    @DeleteMapping("/{id}")
    @RoleRequired({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.removeById(id);
        return Result.ok();
    }

    /**
     * 二维码标签图片（PNG）
    /**
     * 生成设备二维码 PNG（内容始终按当前配置动态构建，保证手机可访问）
     */
    @GetMapping(value = "/qr/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@PathVariable Long id,
                     @RequestParam(defaultValue = "300") int size) {
        Device device = deviceService.getById(id);
        if (device == null) {
            throw new com.smart.community.common.BusinessException("设备不存在");
        }
        String content = deviceService.buildQrContent(device.getDeviceCode());
        return deviceService.qrImage(content, size);
    }
}
