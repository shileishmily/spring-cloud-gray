package cn.springcloud.gray.client.initialize;

import cn.springcloud.gray.GrayClientHolder;
import cn.springcloud.gray.hook.Hook;
import cn.springcloud.gray.hook.HookRegistory;
import cn.springcloud.gray.hook.StartHook;
import cn.springcloud.gray.local.InstanceLocalInfoObtainer;
import cn.springcloud.gray.model.GrayTrackDefinition;
import cn.springcloud.gray.request.track.GrayTrackHolder;
import cn.springcloud.gray.utils.SpringApplicationContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Objects;

/**
 * @author saleson
 * @date 2020-08-16 17:02
 */
@Slf4j
@Order(value = -1)
public class GrayClientApplicationRunner implements ApplicationRunner {
    private GrayInfosInitializer grayInfosInitializer;
    private ApplicationEventPublisher applicationEventPublisher;
    private ApplicationContext applicationContext;
    private HookRegistory hookRegistory;

    public GrayClientApplicationRunner(
            ApplicationContext applicationContext,
            GrayInfosInitializer grayInfosInitializer,
            ApplicationEventPublisher applicationEventPublisher,
            HookRegistory hookRegistory) {
        this.grayInfosInitializer = grayInfosInitializer;
        this.applicationEventPublisher = applicationEventPublisher;
        this.applicationContext = applicationContext;
        this.hookRegistory = hookRegistory;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        loadLocalInstanceInfo();
        loadHooks();
        loadDefaultTrackDefinitions();
        log.info("开始装载灰度...");
        initializeGrayInfos();
        log.info("灰度装载完成.");
        executeStartHooks();
    }

    public boolean initializeGrayInfos() {
        log.info("开始加载灰度信息...");
        try {
            grayInfosInitializer.setup();
        } catch (BeansException e) {
            log.warn("灰度信息加载失败,cause:{}", e.getMessage());
            return false;
        }
        log.info("灰度信息加载完成.");
        return true;
    }

    private void loadLocalInstanceInfo() {
        log.info("开始加载InstanceLocalInfo...");
        InstanceLocalInfoObtainer instanceLocalInfoObtainer =
                SpringApplicationContextUtils.getBean(applicationContext, "instanceLocalInfoInitiralizer", InstanceLocalInfoObtainer.class);
        if (instanceLocalInfoObtainer == null) {
            log.warn("加载InstanceLocalInfo失败, 没有找到InstanceLocalInfoObtainer.");
            return;
        }
        GrayClientHolder.setInstanceLocalInfo(instanceLocalInfoObtainer.getInstanceLocalInfo());
        log.info("加载InstanceLocalInfo完成.");
    }

    private void loadDefaultTrackDefinitions() {
        log.info("开始加载默认的灰度追踪...");
        GrayTrackHolder grayTrackHolder = GrayClientHolder.getGrayTrackHolder();
        GrayTrackDefinition trackDefinition = new GrayTrackDefinition();
        trackDefinition.setName("HttpReceive");
        trackDefinition.setValue("");
        grayTrackHolder.updateTrackDefinition(trackDefinition);
        log.info("加载默认的灰度追踪: {}", trackDefinition.getName());
        log.info("加载默认的灰度追踪完成.");
    }

    private void loadHooks() {
        List<Hook> hooks = SpringApplicationContextUtils.getBeans(applicationContext, Hook.class);
        OrderComparator.sort(hooks);
        hookRegistory.registerHooks(hooks);
    }

    private void executeStartHooks() {
        List<StartHook> startHooks = hookRegistory.getStartHooks();
        if (Objects.isNull(startHooks) || startHooks.isEmpty()) {
            log.info("没有需要执行的StartHook.");
            return;
        }
        log.info("开始执行StartHook...");
        startHooks.forEach(startHook -> {
            log.info("执行StartHook: {}...", startHook.getClass());
            startHook.start();
            log.info("StartHook: {} 执行成功.", startHook.getClass());
        });
        log.info("所有StartHook执行完成.");
    }

}
