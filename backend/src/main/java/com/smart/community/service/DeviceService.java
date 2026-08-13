package com.smart.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.smart.community.common.BusinessException;
import com.smart.community.common.PageResult;
import com.smart.community.entity.Device;
import com.smart.community.mapper.DeviceMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeviceService extends ServiceImpl<DeviceMapper, Device> {

    @Value("${smart.qr.base-url}")
    private String qrBaseUrl;

    public PageResult<Device> page(int page, int size, String keyword, String type, String status) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        LambdaQueryWrapper<Device> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Device::getName, keyword)
                    .or().like(Device::getDeviceCode, keyword)
                    .or().like(Device::getManufacturer, keyword)
                    .or().like(Device::getLocation, keyword));
        }
        if (StringUtils.hasText(type)) {
            qw.eq(Device::getType, type);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Device::getStatus, status);
        }
        qw.orderByAsc(Device::getDeviceCode);
        PageResult<Device> result = PageResult.of(page(new Page<>(page, size), qw));
        // 二维码内容按当前配置动态刷新，避免与 base-url 配置漂移
        result.getRecords().forEach(d -> d.setQrCode(buildQrContent(d.getDeviceCode())));
        return result;
    }

    public Device add(Device device) {
        if (!StringUtils.hasText(device.getDeviceCode())) {
            throw new BusinessException("设备编号不能为空");
        }
        if (getByCode(device.getDeviceCode()) != null) {
            throw new BusinessException("设备编号已存在");
        }
        if (device.getStatus() == null) {
            device.setStatus("RUNNING");
        }
        if (device.getServiceLifeYears() == null) {
            device.setServiceLifeYears(10);
        }
        if (device.getInspectCycle() == null) {
            device.setInspectCycle(15);
        }
        if (device.getMaintainCycle() == null) {
            device.setMaintainCycle(90);
        }
        device.setQrCode(buildQrContent(device.getDeviceCode()));
        save(device);
        return device;
    }

    @Transactional(rollbackFor = Exception.class)
    public Device updateDevice(Device device) {
        Device exist = getById(device.getId());
        if (exist == null) {
            throw new BusinessException("设备不存在");
        }
        // 二维码由服务端统一维护：合并回原值（不依赖全局更新策略），编号变更时同步刷新
        device.setQrCode(null);
        String nextCode = StringUtils.hasText(device.getDeviceCode()) ? device.getDeviceCode() : exist.getDeviceCode();
        if (StringUtils.hasText(device.getDeviceCode())) {
            Device same = getByCode(device.getDeviceCode());
            if (same != null && !same.getId().equals(device.getId())) {
                throw new BusinessException("设备编号已存在");
            }
        }
        device.setQrCode(buildQrContent(nextCode));
        updateById(device);
        return getById(device.getId());
    }

    public Device getByCode(String code) {
        return getOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, code));
    }

    public String buildQrContent(String deviceCode) {
        return qrBaseUrl + "?code=" + deviceCode;
    }

    /**
     * 生成二维码 PNG 字节
     */
    public byte[] qrImage(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("二维码生成失败: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    /**
     * 使用年限信息（用于年限分析）
     */
    public Map<String, Object> lifeInfo(Device d) {
        Map<String, Object> m = new HashMap<>();
        LocalDate install = d.getInstallDate() == null ? LocalDate.now() : d.getInstallDate();
        int age = Period.between(install, LocalDate.now()).getYears();
        int life = d.getServiceLifeYears() == null ? 10 : d.getServiceLifeYears();
        m.put("ageYears", age);
        m.put("lifeYears", life);
        m.put("overdue", age > life);
        m.put("remainYears", Math.max(0, life - age));
        return m;
    }

    public List<Device> listRunning() {
        return list(new LambdaQueryWrapper<Device>()
                .in(Device::getStatus, "RUNNING", "FAULT", "REPAIRING")
                .orderByAsc(Device::getDeviceCode));
    }
}
