package edu.asu.cse564.cse564_project.services;

import edu.asu.cse564.cse564_project.domain.EvidenceCaptureResult;
import edu.asu.cse564.cse564_project.domain.SpeedContext;
import org.springframework.stereotype.Service;

/*
 * Evidence Capture Controller (ECC)
 *
 * 输入：
 *   - SpeedContext（来自 SpeedViolationController）
 *
 * 输出：
 *   - captureActive（单一布尔指令，驱动 Camera + Flash）
 *       TRUE  -> 开始 / 保持捕捉（两者都开启）
 *       FALSE -> 停止捕捉 / 进入休眠（两者都关闭）
 *       null  -> 本次不发任何命令（保持当前状态）
 *   - speedContext（仅在需要打包证据时转发给 EvidenceCollectorAndPackager）
 *
 * 距离逻辑（基于 distanceMeters）：
 *
 *   1) distance <= -20m
 *      - 车辆尚未进入抓拍区（还在设备上游的监控区之内或之外）
 *      - ECC 不改变 Camera/Flash 状态，也不转发上下文
 *      => captureActive = null
 *      => speedContext  = null
 *
 *   2) -20m < distance < 20m
 *      - 车辆处于抓拍窗口
 *      - ECC 打开或保持 Camera + Flash 处于捕捉状态
 *      - 同时将当前 SpeedContext 交给后续打包器
 *      => captureActive = TRUE
 *      => speedContext  = context
 *
 *   3) distance >= 20m
 *      - 车辆已经离开抓拍区
 *      - ECC 明确发出“停止捕捉”的指令，让 Camera + Flash 进入休眠
 *      - 本次不再传递新的上下文
 *      => captureActive = FALSE
 *      => speedContext  = null
 *
 * 该实现假设（方案 C）：
 *   - 对于每一辆被识别/跟踪的车辆，一旦它离开监控/抓拍区域，
 *     SpeedViolationController 一定会发送至少一次携带 distanceMeters >= 20m
 *     的 SpeedContext 给 ECC，从而保证 ECC 有机会发出 captureActive = FALSE。
 */
@Service
public class EvidenceCaptureControllerService {

    // 抓拍窗口边界（单位：米）
    private static final double CAPTURE_WINDOW_METERS = 20.0;

    /**
     * Handle a SpeedContext from SpeedViolationController and decide:
     *   - 是否需要改变 Camera + Flash 的捕捉状态
     *   - 是否要将该 context 转发给证据打包组件
     *
     * @param context SpeedContext（可包含 overspeed=true/false，ECC 不依赖该字段）
     * @return EvidenceCaptureResult，描述该次样本对应的控制决策
     */
    public EvidenceCaptureResult handleSpeedContext(SpeedContext context) {
        if (context == null) {
            // 没有上下文就什么都不做
            return EvidenceCaptureResult.builder().build();
        }

        double distanceMeters = context.getDistanceMeters();

        // Case 1: 车辆尚未进入抓拍区（<= -20m） → 不发任何命令，不转发上下文
        if (distanceMeters <= -CAPTURE_WINDOW_METERS) {
            return EvidenceCaptureResult.builder()
                    .captureActive(null)   // 不改变 Camera/Flash 状态
                    .speedContext(null)    // 不交给打包器
                    .build();
        }

        // Case 2: 车辆在抓拍窗口内（-20m ~ 20m）→ 开启/保持捕捉，并转发上下文
        if (distanceMeters > -CAPTURE_WINDOW_METERS && distanceMeters < CAPTURE_WINDOW_METERS) {
            return EvidenceCaptureResult.builder()
                    .captureActive(Boolean.TRUE)  // Camera + Flash 应该处于工作状态
                    .speedContext(context)        // 将该样本交给打包器，用于构造证据
                    .build();
        }

        // Case 3: 车辆已经离开抓拍区（>= 20m）→ 明确发出“停止捕捉”的指令
        return EvidenceCaptureResult.builder()
                .captureActive(Boolean.FALSE) // Camera + Flash 停止工作，进入休眠
                .speedContext(null)           // 本次不再传递上下文
                .build();
    }
}
