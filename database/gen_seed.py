# -*- coding: utf-8 -*-
"""
智慧社区设备设施全生命周期管理系统 - 真实感种子数据生成器
生成 database/smart_community.sql（建表 + 演示数据）
运行：python gen_seed.py
"""
import json
import random
from datetime import date, datetime, timedelta

random.seed(20260809)

TODAY = date.today()
# 二维码内容前缀：需与 backend/src/main/resources/application.yml 中 smart.qr.base-url 一致
# 手机扫码演示时改为电脑局域网地址；更换网络后需同步修改两处
QR_BASE = "http://172.20.10.5:5173/mobile/scan?code="

# ---------------- 用户 ----------------
USERS = [
    ("admin", "张伟", "ADMIN", "13801234567"),
    ("inspector", "陈志强", "INSPECTOR", "13922334455"),
    ("sunlh", "孙丽华", "INSPECTOR", "13766778899"),
    ("maintainer", "刘建国", "MAINTAINER", "13655667788"),
    ("zhaogq", "赵国强", "MAINTAINER", "13588990011"),
]

INSPECTORS = ["陈志强", "孙丽华"]
MAINTAINERS = ["刘建国", "赵国强"]

# ---------------- 设备 ----------------
# code, name, type, model, manufacturer, location, install, life, warranty_end, status, ic, mc, spec, remark
ELEVATOR_MODELS = [
    ("KONE MiniSpace", "通力电梯", "1.75m/s"),
    ("OTIS Gen2", "奥的斯电梯", "1.6m/s"),
    ("日立 HGP", "日立电梯", "1.75m/s"),
    ("三菱 LEHY-II", "三菱电机", "1.6m/s"),
    ("迅达 5500", "迅达电梯", "2.0m/s"),
]
ELEVATOR_LOADS = [800, 1000, 1050, 1350]

DEVICES = []
DEVICE_SEQ = {"ELEVATOR": 1, "FIRE": 1, "PUMP": 1, "ACCESS": 1, "OTHER": 1}

# 电梯：1-8号楼
ELEV_LOCATIONS = [
    ("1号楼1单元", "1号楼2单元"), ("2号楼1单元", "2号楼2单元"),
    ("3号楼1单元", "3号楼2单元", "3号楼3单元"), ("4号楼1单元", "4号楼2单元"),
    ("5号楼1单元", "5号楼2单元", "5号楼货梯"), ("6号楼1单元", "6号楼2单元"),
    ("7号楼1单元", "7号楼2单元"), ("8号楼1单元", "8号楼2单元"),
]
ele_locs = [x for grp in ELEV_LOCATIONS for x in grp]  # 15 处
for i, loc in enumerate(ele_locs[:12]):
    model, mfr, spd = random.choice(ELEVATOR_MODELS)
    if "货梯" in loc:
        load, spd = 1600, "0.75m/s"
    else:
        load = random.choice(ELEVATOR_LOADS)
    floors = random.choice(["12层/12站", "18层/18站", "24层/24站", "11层/11站"])
    name = loc if "货梯" in loc else loc + "客梯"
    install = date(2012 + random.randint(0, 6), random.randint(1, 12), random.randint(1, 28))
    if i >= 9:
        install = date(2022 + random.randint(0, 3), random.randint(1, 12), random.randint(1, 28))
    spec = {"额定载重": f"{load}kg", "额定速度": spd, "层站": floors,
            "曳引机": random.choice(["永磁同步", "蜗轮蜗杆"]), "开门方式": "中分门"}
    DEVICES.append(["ELEVATOR", f"DT-{DEVICE_SEQ['ELEVATOR']:03d}", name, model, mfr, loc,
                    install, 15, install + timedelta(days=365 * (2 if random.random() < .5 else 3)), 15, 90, spec])
    DEVICE_SEQ["ELEVATOR"] += 1

# 消防
FIRE_DEV = [
    ("火灾自动报警系统", "GST-5000", "海湾消防", "监控中心", 2016, "2回路/512点"),
    ("消防联动控制系统", "GST-LD", "海湾消防", "监控中心", 2016, "联动模块/128点"),
    ("室内消火栓系统", "XFS-100", "中消安", "全区楼道", 2014, "1.6MPa"),
    ("自动喷淋系统", "ZSP-100", "中消安", "地下车库", 2017, "1.2MPa"),
    ("防排烟系统", "PY-01", "上虞风机", "全区", 2015, "11kW"),
    ("消防应急照明", "SZSW-201", "海湾消防", "全区", 2019, "疏散指示/420点"),
    ("气体灭火系统", "QMH-70", "泰和安", "配电房", 2018, "七氟丙烷/70L"),
    ("消防广播系统", "GB-5000", "霍尼韦尔", "全区", 2016, "功放/240W"),
]
for name, model, mfr, loc, yr, spec_txt in FIRE_DEV:
    install = date(yr, random.randint(1, 12), random.randint(1, 28))
    spec = {"系统构成": spec_txt, "控制方式": "集中控制", "供电方式": "双回路供电"}
    DEVICES.append(["FIRE", f"XF-{DEVICE_SEQ['FIRE']:03d}", name, model, mfr, loc,
                    install, 10, install + timedelta(days=365 * 3), 30, 180, spec])
    DEVICE_SEQ["FIRE"] += 1

# 水泵
PUMP_DEV = [
    ("1号生活水泵", "CDL42-30", "南方泵业", "地下泵房", 2014, "42m³/h", "30m"),
    ("2号生活水泵", "CDL42-30", "南方泵业", "地下泵房", 2014, "42m³/h", "30m"),
    ("3号生活水泵", "CDL65-40", "格兰富", "地下泵房", 2019, "65m³/h", "40m"),
    ("1号消防泵", "XBD8.0/30", "凯泉泵业", "地下泵房", 2016, "108m³/h", "80m"),
    ("2号消防泵", "XBD8.0/30", "凯泉泵业", "地下泵房", 2016, "108m³/h", "80m"),
    ("1号稳压泵", "ZW(L)-I-X-10", "威乐", "地下泵房", 2018, "3m³/h", "100m"),
    ("2号稳压泵", "ZW(L)-I-X-10", "威乐", "地下泵房", 2018, "3m³/h", "100m"),
    ("中区给水泵", "CDL32-20", "南方泵业", "地下泵房", 2017, "32m³/h", "20m"),
    ("排污泵(1#集水井)", "WQ25-15", "凯泉泵业", "地下车库", 2016, "25m³/h", "15m"),
]
for name, model, mfr, loc, yr, flow, head in PUMP_DEV:
    install = date(yr, random.randint(1, 12), random.randint(1, 28))
    kw = random.choice(["7.5kW", "11kW", "15kW", "5.5kW"])
    spec = {"额定流量": flow, "扬程": head, "电机功率": kw, "转速": "2950r/min"}
    DEVICES.append(["PUMP", f"SB-{DEVICE_SEQ['PUMP']:03d}", name, model, mfr, loc,
                    install, 8 if "生活" in name or "中区" in name else 10,
                    install + timedelta(days=365 * 2), 15, 90, spec])
    DEVICE_SEQ["PUMP"] += 1

# 门禁
ACCESS_DEV = [
    ("北门人行门禁", "DS-K1T671", "海康威视", "北门岗亭", 2020, "人脸+刷卡"),
    ("南门人行门禁", "DS-K1T671", "海康威视", "南门岗亭", 2020, "人脸+刷卡"),
    ("南门车行道闸", "DS-TMG520", "海康威视", "南门", 2020, "车牌识别"),
    ("东门车行道闸", "DS-TMG520", "海康威视", "东门", 2021, "车牌识别"),
    ("1号地下车库门禁", "DS-K1T671", "海康威视", "地下车库A口", 2020, "人脸+刷卡"),
    ("2号地下车库门禁", "DS-K1T671", "海康威视", "地下车库B口", 2020, "人脸+刷卡"),
    ("单元门禁主机(3号楼)", "DS-KH9300", "海康威视", "3号楼各单元", 2019, "可视对讲"),
    ("梯控系统", "DS-K2M0516", "海康威视", "各楼栋电梯轿厢", 2021, "IC卡+二维码"),
]
for name, model, mfr, loc, yr, mode in ACCESS_DEV:
    install = date(yr, random.randint(1, 12), random.randint(1, 28))
    spec = {"识别方式": mode, "通讯方式": "TCP/IP", "供电": "DC12V"}
    DEVICES.append(["ACCESS", f"MJ-{DEVICE_SEQ['ACCESS']:03d}", name, model, mfr, loc,
                    install, 6, install + timedelta(days=365), 30, 180, spec])
    DEVICE_SEQ["ACCESS"] += 1

# 其他
OTHER_DEV = [
    ("小区视频监控系统", "DS-9600N", "海康威视", "全区", 2014, 8, "264路/16盘位"),
    ("周界报警系统", "DS-PWA32", "海康威视", "围墙周界", 2019, 8, "32防区"),
    ("应急发电机", "KC-300", "康明斯", "配电房", 2015, 15, "300kW"),
    ("小区变压器(1#)", "S11-M-630", "正泰电气", "配电房", 2015, 20, "630kVA"),
    ("小区变压器(2#)", "S11-M-400", "正泰电气", "配电房", 2018, 20, "400kVA"),
    ("东区充电桩", "AC-7KW", "特来电", "东区停车场", 2022, 8, "7kW/双枪"),
    ("西区充电桩", "AC-7KW", "特来电", "西区停车场", 2022, 8, "7kW/双枪"),
    ("中央空调机组", "LSBLG340", "格力中央空调", "物业楼", 2017, 12, "制冷量340kW"),
    ("水泵房控制柜", "XLL2", "正泰电气", "地下泵房", 2014, 10, "一用一备"),
]
for name, model, mfr, loc, yr, life, spec_txt in OTHER_DEV:
    install = date(yr, random.randint(1, 12), random.randint(1, 28))
    spec = {"主要参数": spec_txt, "安装位置": loc}
    DEVICES.append(["OTHER", f"QT-{DEVICE_SEQ['OTHER']:03d}", name, model, mfr, loc,
                    install, life, install + timedelta(days=365 * 3), 30, 90 if "发电机" in name else 180, spec])
    DEVICE_SEQ["OTHER"] += 1

# 状态：大部分运行，少数故障/维修/停用/报废
random.shuffle(DEVICES)
STATUS_ROLL = {0: "RUNNING", 1: "RUNNING", 2: "RUNNING", 3: "RUNNING", 4: "RUNNING",
               5: "RUNNING", 6: "RUNNING", 7: "RUNNING", 8: "RUNNING", 9: "RUNNING",
               10: "RUNNING", 11: "RUNNING", 12: "RUNNING", 13: "RUNNING", 14: "RUNNING",
               15: "RUNNING", 16: "RUNNING", 17: "RUNNING", 18: "RUNNING", 19: "RUNNING",
               20: "RUNNING", 21: "RUNNING", 22: "RUNNING", 23: "RUNNING", 24: "RUNNING",
               25: "RUNNING", 26: "RUNNING", 27: "RUNNING", 28: "RUNNING", 29: "RUNNING",
               30: "RUNNING", 31: "RUNNING", 32: "RUNNING", 33: "RUNNING", 34: "RUNNING",
               35: "RUNNING", 36: "RUNNING", 37: "RUNNING", 38: "RUNNING", 39: "RUNNING",
               40: "RUNNING", 41: "RUNNING", 42: "RUNNING", 43: "FAULT", 44: "FAULT",
               45: "REPAIRING", 46: "REPAIRING", 47: "STOPPED", 48: "SCRAPPED"}
for i, d in enumerate(DEVICES):
    d.append(STATUS_ROLL.get(i, "RUNNING"))

# 补充备注
REMARKS = {
    "FAULT": "存在故障，已报修处理中",
    "REPAIRING": "正在维修，暂停使用",
    "STOPPED": "设备停用，待大修评估",
    "SCRAPPED": "已达报废年限，已启动更新采购",
}

# ---------------- 检查项模板（按设备类型） ----------------
CHECK_ITEMS = {
    "ELEVATOR": [("轿厢照明与通风", "轿厢按钮面板", "平层精度", "开关门运行", "五方对讲", "钢丝绳磨损"),
                 ("轿厢内应急照明", "超载保护", "运行噪音")],
    "FIRE": [("报警主机运行", "探测器巡检", "手报按钮", "消防泵压力", "消防广播"),
             ("应急照明", "疏散指示", "防火门状态")],
    "PUMP": [("泵体运行声音", "密封渗漏", "压力表读数", "电机温升", "控制柜指示"),
             ("接地保护", "阀门开闭")],
    "ACCESS": [("读卡识别", "道闸起落", "摄像头画面", "门体闭合"), ("电源状态", "网络通讯")],
    "OTHER": [("设备运行声音", "温度检查", "接线端子", "外壳清洁"), ("指示灯状态", "接地检查")],
}

INSPECT_REMARKS_NORMAL = ["运行平稳，无异常", "各项指标正常", "检查无异常", "设备运行正常", "润滑到位，状态良好"]
INSPECT_REMARKS_ABNORMAL = ["发现轻微异响，建议关注", "密封处有渗水痕迹", "指示灯闪烁异常", "紧固件有松动", "表面锈蚀，需除锈处理"]
MAINTAIN_REMARKS = ["完成润滑保养", "更换磨损件", "紧固全部接线", "清洁滤网", "加注润滑油，更换密封圈"]

# 异常项组合
ABNORMAL_ITEMS = {
    "ELEVATOR": [("开关门运行", "开关门有轻微卡顿"), ("运行噪音", "轿厢运行噪音偏大"), ("钢丝绳磨损", "钢丝绳表面有毛刺")],
    "FIRE": [("探测器巡检", "2个探测器报警延迟"), ("消防泵压力", "稳压泵频繁启动")],
    "PUMP": [("泵体运行声音", "轴承处有异响"), ("密封渗漏", "机械密封渗水"), ("压力表读数", "压力波动偏大")],
    "ACCESS": [("读卡识别", "读卡偶发失败"), ("道闸起落", "道闸起落缓慢")],
    "OTHER": [("温度检查", "端子温度偏高"), ("设备运行声音", "运行噪音略大")],
}

# ---------------- 报修人/电话 ----------------
REPORTERS = ["刘淑芬", "王志强", "李秀英", "张建国", "陈美玲", "杨永刚", "赵丽娟", "孙德福",
             "周桂兰", "吴海燕", "郑学军", "王桂英", "冯志明", "许春华", "何国栋", "林晓芳",
             "罗永康", "宋玉梅", "谢志刚", "韩秀兰", "唐文斌", "曹丽萍", "邓建华", "萧美云"]
REPORTER_PHONES = ["13{}".format("".join(random.choices("0123456789", k=9))) for _ in range(30)]

FAULT_TEMPLATES = {
    "ELEVATOR": [
        ("{}运行至顶层时{}", ["有异响", "轻微抖动", "顿挫感明显"]),
        ("{}客梯{}", ["按键失灵，面板灯不亮", "开门时噪音大", "平层不准，高出地面约3cm", "轿厢照明闪烁", "五方对讲无应答"]),
        ("{}客梯困人报警后无法复位", []),
    ],
    "FIRE": [
        ("{}报警主机{}", ["频繁误报", "显示故障代码E12", "个别探测器离线"]),
        ("{}楼道{}", ["消火栓箱门破损", "应急指示灯不亮", "喷淋头渗水"]),
    ],
    "PUMP": [
        ("{}运行{}", ["噪音大", "有渗水现象", "压力不稳"]),
        ("{}控制柜{}", ["指示灯不亮", "频繁跳闸"]),
    ],
    "ACCESS": [
        ("{}无法{}", ["刷卡识别", "识别车牌", "正常起落杆"]),
        ("{}{}", ["读卡器故障", "门禁主机黑屏", "道闸不落杆"]),
    ],
    "OTHER": [
        ("{}运行异常", []),
        ("{}{}", ["画面模糊，需检修", "异响", "过热保护跳闸"]),
    ],
}

FIX_TEMPLATES = {
    "ELEVATOR": ["更换按钮面板，测试正常", "调整导轨间隙并重新润滑", "更换平层感应器并校准", "更换轿厢照明镇流器",
                 "检查门机皮带并更换", "复位困人报警系统，测试正常", "更换五方对讲主机"],
    "FIRE": ["更换破损箱门及玻璃", "更换探测器并重新编码", "主机程序升级，误报消除", "更换应急指示灯"],
    "PUMP": ["更换机械密封并注水测试", "更换轴承，运行平稳", "调整压力开关阈值", "更换控制柜接触器"],
    "ACCESS": ["更换读卡器，测试通过", "调整道闸限位开关", "更换门禁电源模块", "重启主机并升级固件"],
    "OTHER": ["更换老化线路端子", "清洁滤网并补充制冷剂", "更换散热风扇，温度恢复正常", "更换启动电瓶"],
}

# ---------------- 合同 ----------------
CONTRACTS = [
    ("HT-2025-006", "电梯维保合同（1-4号楼）", "ELEVATOR", "通力电梯有限公司", "2025-09-01", 42000, "张经理", "13701880001", "半年付", "包含年度检验配合"),
    ("HT-2025-007", "电梯维保合同（5-8号楼）", "ELEVATOR", "奥的斯电梯（中国）有限公司", "2025-10-01", 46800, "李经理", "13701880002", "季付", None),
    ("HT-2026-001", "消防设施维保合同", "FIRE", "中消安消防工程有限公司", "2026-03-01", 36000, "刘工", "13701880003", "季付", "含年度检测"),
    ("HT-2026-002", "水泵机组维保合同", "PUMP", "凯泉泵业售后服务中心", "2026-01-15", 18500, "陈工", "13701880004", "半年付", None),
    ("HT-2026-003", "门禁系统维保合同", "ACCESS", "海康威视服务中心", "2026-06-01", 12800, "王工", "13701880005", "年付", None),
    ("HT-2024-009", "监控系统维保合同", "OTHER", "海康威视服务中心", "2024-05-01", 15000, "王工", "13701880005", "年付", "已到期未续签"),
    ("HT-2026-004", "中央空调维保合同", "OTHER", "格力售后服务中心", "2026-05-15", 22000, "周工", "13701880006", "季付", "含换季保养两次"),
    ("HT-2025-008", "发电机维保合同", "OTHER", "康明斯动力服务部", "2025-06-01", 9600, "吴工", "13701880007", "年付", "已到期未续签"),
]

# ---------------- 生成 SQL ----------------
def fmt_date(d):
    return d.strftime("%Y-%m-%d")

def fmt_dt(dt):
    return dt.strftime("%Y-%m-%d %H:%M:%S")

def esc(s):
    if s is None:
        return "NULL"
    return "'" + str(s).replace("'", "''") + "'"

def build():
    lines = []
    lines.append("-- =====================================================================")
    lines.append("-- 智慧社区设备设施全生命周期管理系统 - 数据库初始化脚本（种子数据已生成）")
    lines.append("-- 生成时间: %s" % datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    lines.append("-- =====================================================================")
    lines.append("DROP DATABASE IF EXISTS smart_community;")
    lines.append("CREATE DATABASE smart_community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;")
    lines.append("USE smart_community;")
    lines.append("")

    # ---------- DDL ----------
    DDL = """
CREATE TABLE sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  username    VARCHAR(50)  NOT NULL,
  password    VARCHAR(64)  NOT NULL COMMENT 'MD5',
  real_name   VARCHAR(50)  NOT NULL,
  role        VARCHAR(20)  NOT NULL DEFAULT 'INSPECTOR',
  phone       VARCHAR(20)  DEFAULT NULL,
  avatar      VARCHAR(255) DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='系统用户表';

CREATE TABLE device (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  device_code        VARCHAR(50)  NOT NULL,
  name               VARCHAR(100) NOT NULL,
  type               VARCHAR(20)  NOT NULL,
  model              VARCHAR(100) DEFAULT NULL,
  manufacturer       VARCHAR(100) DEFAULT NULL,
  location           VARCHAR(200) DEFAULT NULL,
  install_date       DATE         DEFAULT NULL,
  service_life_years INT          NOT NULL DEFAULT 10,
  warranty_end       DATE         DEFAULT NULL,
  status             VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
  inspect_cycle      INT          NOT NULL DEFAULT 15,
  maintain_cycle     INT          NOT NULL DEFAULT 90,
  spec               TEXT         DEFAULT NULL COMMENT '技术参数(JSON)',
  qr_code            VARCHAR(500) DEFAULT NULL,
  remark             VARCHAR(500) DEFAULT NULL,
  create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_code (device_code),
  KEY idx_type (type),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='设备台账表';

CREATE TABLE device_task (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  task_code   VARCHAR(50)  NOT NULL,
  task_type   VARCHAR(20)  NOT NULL,
  device_id   BIGINT       NOT NULL,
  plan_date   DATE         NOT NULL,
  executor    VARCHAR(50)  DEFAULT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  result      VARCHAR(20)  DEFAULT NULL,
  check_time  DATETIME     DEFAULT NULL,
  location    VARCHAR(200) DEFAULT NULL,
  photo       VARCHAR(500) DEFAULT NULL,
  check_items TEXT         DEFAULT NULL COMMENT '检查项明细(JSON)',
  remark      VARCHAR(500) DEFAULT NULL,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_code (task_code),
  KEY idx_device (device_id),
  KEY idx_status (status),
  KEY idx_plan (plan_date)
) ENGINE=InnoDB COMMENT='巡检/保养任务表';

CREATE TABLE repair_order (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  order_code  VARCHAR(50)   NOT NULL,
  device_id   BIGINT        NOT NULL,
  reporter    VARCHAR(50)   NOT NULL,
  phone       VARCHAR(20)   DEFAULT NULL,
  fault_desc  VARCHAR(500)  NOT NULL,
  level       VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
  status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  assignee    VARCHAR(50)   DEFAULT NULL,
  fix_result  VARCHAR(500)  DEFAULT NULL,
  cost        DECIMAL(10,2) DEFAULT NULL,
  fix_hours   DECIMAL(4,1)  DEFAULT NULL COMMENT '维修工时(小时)',
  create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
  assign_time DATETIME      DEFAULT NULL,
  finish_time DATETIME      DEFAULT NULL,
  verify_time DATETIME      DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_code (order_code),
  KEY idx_status (status),
  KEY idx_device (device_id)
) ENGINE=InnoDB COMMENT='报修工单表';

CREATE TABLE contract (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  contract_no   VARCHAR(50)   NOT NULL,
  contract_name VARCHAR(100)  NOT NULL,
  device_type   VARCHAR(20)   NOT NULL,
  vendor        VARCHAR(100)  NOT NULL,
  start_date    DATE          NOT NULL,
  end_date      DATE          NOT NULL,
  amount        DECIMAL(12,2) DEFAULT NULL,
  contact       VARCHAR(50)   DEFAULT NULL,
  contact_phone VARCHAR(20)   DEFAULT NULL,
  pay_method    VARCHAR(50)   DEFAULT NULL COMMENT '付款方式',
  status        VARCHAR(20)   NOT NULL DEFAULT 'VALID',
  remark        VARCHAR(500)  DEFAULT NULL,
  create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_no (contract_no)
) ENGINE=InnoDB COMMENT='维保合同表';

CREATE TABLE device_log (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  device_id   BIGINT        NOT NULL,
  metric      VARCHAR(20)   NOT NULL,
  value       DECIMAL(10,2) NOT NULL,
  record_time DATETIME      NOT NULL,
  PRIMARY KEY (id),
  KEY idx_device_time (device_id, record_time)
) ENGINE=InnoDB COMMENT='设备运行日志表';
"""
    lines.append(DDL)

    # ---------- 用户 ----------
    lines.append("-- 用户（密码均为 123456）")
    lines.append("INSERT INTO sys_user (username, password, real_name, role, phone) VALUES")
    pw = "e10adc3949ba59abbe56e057f20f883e"
    rows = [f"({esc(u)}, '{pw}', {esc(n)}, {esc(r)}, {esc(p)})" for u, n, r, p in USERS]
    lines.append(",\n".join(rows) + ";")
    lines.append("")

    # ---------- 设备 ----------
    lines.append("-- 设备台账（%d 台）" % len(DEVICES))
    lines.append("INSERT INTO device (device_code, name, type, model, manufacturer, location, install_date, service_life_years, warranty_end, status, inspect_cycle, maintain_cycle, spec, qr_code, remark) VALUES")
    dev_rows = []
    for idx, device in enumerate(DEVICES):
        dtype, code, name, model, mfr, loc, install, life, warranty, ic, mc, spec, status = device
        remark = REMARKS.get(status)
        if status == "RUNNING" and dtype == "PUMP" and "2014" in str(install):
            remark = "超期服役设备，已列入更新计划"
        spec_json = json.dumps(spec, ensure_ascii=False)
        dev_rows.append(f"({esc(code)}, {esc(name)}, {esc(dtype)}, {esc(model)}, {esc(mfr)}, {esc(loc)}, {esc(fmt_date(install))}, {life}, {esc(fmt_date(warranty))}, {esc(status)}, {ic}, {mc}, {esc(spec_json)}, {esc(QR_BASE + code)}, {esc(remark)})")
    lines.append(",\n".join(dev_rows) + ";")
    lines.append("")

    # ---------- 任务 ----------
    task_rows = []
    code_seq = {}

    def task_code(prefix, d):
        key = (prefix, d)
        code_seq[key] = code_seq.get(key, 0) + 1
        return f"{prefix}{d.strftime('%Y%m%d')}{code_seq[key]:03d}"

    def gen_tasks(device, idx, phase_seed):
        dtype, code, name, model, mfr, loc, install, life, warranty, ic, mc, spec, status = device
        executor = random.choice(INSPECTORS if dtype in ("ELEVATOR", "PUMP") else INSPECTORS)
        # 巡检历史 + 未来
        start = TODAY - timedelta(days=170)
        # 相位偏移让各设备巡检日错开
        phase = (idx * 3 + phase_seed) % ic
        d = start + timedelta(days=phase)
        while d <= TODAY + timedelta(days=30):
            ttype = "INSPECT"
            if d <= TODAY:
                if random.random() < 0.055:
                    status_t, result_t = "OVERDUE", None
                    check_time = None
                    remark = "未按时完成，已提醒补巡"
                    items = None
                else:
                    status_t = "COMPLETED"
                    abnormal = random.random() < 0.13
                    result_t = "ABNORMAL" if abnormal else "NORMAL"
                    check_time = datetime(d.year, d.month, d.day, random.randint(8, 17), random.choice([5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55]), random.randint(0, 59))
                    if abnormal:
                        it, msg = random.choice(ABNORMAL_ITEMS[dtype])
                        items = json.dumps([{"name": n, "result": "异常" if n == it else "正常", "detail": msg if n == it else ""} for n in random.choice(CHECK_ITEMS[dtype])], ensure_ascii=False)
                        remark = msg
                    else:
                        items = json.dumps([{"name": n, "result": "正常", "detail": ""} for n in random.choice(CHECK_ITEMS[dtype])], ensure_ascii=False)
                        remark = random.choice(INSPECT_REMARKS_NORMAL)
                loc_t = loc + random.choice(["", " 一层", " 顶层", " 设备层"])
                exec_sql = esc(executor) if status_t != "PENDING" else "NULL"
                ct_sql = esc(fmt_dt(check_time)) if check_time else "NULL"
                res_sql = esc(result_t) if result_t else "NULL"
                items_sql = esc(items) if items else "NULL"
                task_rows.append(f"({esc(task_code('XJ', d))}, 'INSPECT', {idx + 1}, '{fmt_date(d)}', {exec_sql}, '{status_t}', {res_sql}, {ct_sql}, {esc(loc_t)}, {esc(remark)}, {items_sql})")
            else:
                task_rows.append(f"({esc(task_code('XJ', d))}, 'INSPECT', {idx + 1}, '{fmt_date(d)}', NULL, 'PENDING', NULL, NULL, NULL, NULL, NULL)")
            d += timedelta(days=ic)
        # 保养历史 + 未来
        mstart = TODAY - timedelta(days=170)
        d = mstart + timedelta(days=(idx * 5) % mc)
        while d <= TODAY + timedelta(days=30):
            ttype = "MAINTAIN"
            if d <= TODAY:
                if random.random() < 0.9:
                    check_time = datetime(d.year, d.month, d.day, random.randint(9, 16), random.randint(0, 59), random.randint(0, 59))
                    task_rows.append(f"({esc(task_code('BY', d))}, 'MAINTAIN', {idx + 1}, '{fmt_date(d)}', {esc(random.choice(MAINTAINERS))}, 'COMPLETED', 'NORMAL', {esc(fmt_dt(check_time))}, {esc(loc)}, {esc(random.choice(MAINTAIN_REMARKS))}, NULL)")
                else:
                    task_rows.append(f"({esc(task_code('BY', d))}, 'MAINTAIN', {idx + 1}, '{fmt_date(d)}', NULL, 'OVERDUE', NULL, NULL, {esc(loc)}, {esc('保养未完成，已顺延')}, NULL)")
            else:
                task_rows.append(f"({esc(task_code('BY', d))}, 'MAINTAIN', {idx + 1}, '{fmt_date(d)}', NULL, 'PENDING', NULL, NULL, NULL, NULL, NULL)")
            d += timedelta(days=mc)

    for idx, device in enumerate(DEVICES):
        gen_tasks(device, idx, random.randint(0, 10))

    lines.append("-- 巡检/保养任务（历史 + 计划，共 %d 条）" % len(task_rows))
    lines.append("INSERT INTO device_task (task_code, task_type, device_id, plan_date, executor, status, result, check_time, location, remark, check_items) VALUES")
    lines.append(",\n".join(task_rows) + ";")
    lines.append("")

    # ---------- 工单 ----------
    lines.append("-- 报修工单（%d 条）" % 67)
    order_rows = []
    oseq = {}
    for _ in range(67):
        dtype = random.choices(["ELEVATOR", "PUMP", "FIRE", "ACCESS", "OTHER"], weights=[30, 20, 16, 15, 19])[0]
        candidates = [d for d in DEVICES if d[0] == dtype and d[12] != "SCRAPPED"]
        d = random.choice(candidates)
        dev_id = DEVICES.index(d) + 1
        dev_name = d[1]
        tpls = FAULT_TEMPLATES[dtype]
        t = random.choice(tpls)
        tpl_str = t[0]
        opts = t[1] if len(t) > 1 else []
        if opts:
            fault = tpl_str.format(dev_name, random.choice(opts))
        else:
            fault = tpl_str.format(dev_name)
        # 日期偏近期
        days_ago = int(abs(random.gauss(30, 40)))
        days_ago = min(max(days_ago, 1), 170)
        create_dt = datetime.combine(TODAY - timedelta(days=days_ago), datetime.min.time()) + timedelta(
            hours=random.randint(8, 20), minutes=random.randint(0, 59))
        level = random.choices(["HIGH", "MEDIUM", "LOW"], weights=[22, 55, 23])[0]
        status = random.choices(["VERIFIED", "COMPLETED", "PROCESSING", "PENDING"], weights=[55, 13, 11, 21])[0]
        reporter = random.choice(REPORTERS)
        phone = random.choice(REPORTER_PHONES)
        assignee = random.choice(MAINTAINERS) if status != "PENDING" else "NULL"
        cost = None
        fix_hours = None
        fix_result = "NULL"
        assign_dt = finish_dt = verify_dt = None
        if status != "PENDING":
            assign_dt = create_dt + timedelta(hours=random.randint(1, 20))
        if status in ("COMPLETED", "VERIFIED"):
            finish_dt = assign_dt + timedelta(hours=random.randint(2, 36))
            cost = round(random.uniform({"ELEVATOR": 260, "PUMP": 220, "FIRE": 180, "ACCESS": 120, "OTHER": 400}[dtype],
                                        {"ELEVATOR": 1680, "PUMP": 1280, "FIRE": 980, "ACCESS": 560, "OTHER": 2600}[dtype]), 2)
            fix_hours = round(random.uniform(0.5, 8), 1)
            fix_result = esc(random.choice(FIX_TEMPLATES[dtype]))
        if status == "VERIFIED":
            verify_dt = finish_dt + timedelta(hours=random.randint(6, 72))
        okey = (create_dt.year, create_dt.month)
        oseq[okey] = oseq.get(okey, 0) + 1
        ocode = f"GZ{create_dt.strftime('%Y%m')}{oseq[okey]:03d}"
        cost_sql = cost if cost is not None else "NULL"
        hours_sql = fix_hours if fix_hours is not None else "NULL"
        order_rows.append(f"({esc(ocode)}, {dev_id}, {esc(reporter)}, {esc(phone)}, {esc(fault)}, {esc(level)}, {esc(status)}, {esc(assignee) if assignee != 'NULL' else 'NULL'}, {fix_result}, {cost_sql}, {hours_sql}, {esc(fmt_dt(create_dt))}, {esc(fmt_dt(assign_dt)) if assign_dt else 'NULL'}, {esc(fmt_dt(finish_dt)) if finish_dt else 'NULL'}, {esc(fmt_dt(verify_dt)) if verify_dt else 'NULL'})")
    lines.append("INSERT INTO repair_order (order_code, device_id, reporter, phone, fault_desc, level, status, assignee, fix_result, cost, fix_hours, create_time, assign_time, finish_time, verify_time) VALUES")
    lines.append(",\n".join(order_rows) + ";")
    lines.append("")

    # ---------- 合同 ----------
    lines.append("-- 维保合同（8 份，含临期/过期）")
    lines.append("INSERT INTO contract (contract_no, contract_name, device_type, vendor, start_date, end_date, amount, contact, contact_phone, pay_method, status, remark) VALUES")
    c_rows = []
    for i, (no, cname, dtype, vendor, start, amount, contact, cphone, pay, remark) in enumerate(CONTRACTS):
        start_d = datetime.strptime(start, "%Y-%m-%d").date()
        end_d = start_d + timedelta(days=365)
        if i == 3:
            end_d = TODAY + timedelta(days=12)  # 水泵合同：临期
        if i == 7:
            end_d = TODAY - timedelta(days=35)  # 发电机合同：已过期
        if end_d < TODAY:
            st = "EXPIRED"
        elif end_d <= TODAY + timedelta(days=30):
            st = "EXPIRING"
        else:
            st = "VALID"
        c_rows.append(f"({esc(no)}, {esc(cname)}, {esc(dtype)}, {esc(vendor)}, {esc(fmt_date(start_d))}, {esc(fmt_date(end_d))}, {amount}, {esc(contact)}, {esc(cphone)}, {esc(pay)}, {esc(st)}, {esc(remark)})")
    lines.append(",\n".join(c_rows) + ";")
    lines.append("")

    # ---------- 运行日志 ----------
    lines.append("-- 运行监测日志（主要设备近24小时）")
    log_rows = []
    for dev_id in [1, 2, 3, 5, 9, 10, 12, 13, 17, 18, 20, 21, 22]:
        for h in range(24, 0, -1):
            t = datetime.now() - timedelta(hours=h)
            t = t.replace(minute=random.choice([0, 15, 30, 45]), second=random.randint(0, 59))
            log_rows.append(f"({dev_id}, 'TEMPERATURE', {round(random.uniform(34, 66), 1)}, '{fmt_dt(t)}')")
            log_rows.append(f"({dev_id}, 'VIBRATION', {round(random.uniform(0.3, 2.4), 2)}, '{fmt_dt(t)}')")
    lines.append("INSERT INTO device_log (device_id, metric, value, record_time) VALUES")
    lines.append(",\n".join(log_rows) + ";")
    lines.append("")

    return "\n".join(lines), len(task_rows), len(order_rows)


if __name__ == "__main__":
    sql, n_tasks, n_orders = build()
    out = "smart_community.sql"
    with open(out, "w", encoding="utf-8") as f:
        f.write(sql)
    print("OK -> %s" % out)
    print("devices=%d tasks=%d orders=%d contracts=8" % (len(DEVICES), n_tasks, n_orders))
