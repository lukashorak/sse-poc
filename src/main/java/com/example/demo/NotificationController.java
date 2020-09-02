package com.example.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@RestController
@RequestMapping(path = "/notification")
@Log4j2
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private SseEmitter emitter = new SseEmitter();
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();


    private final ApplicationEventPublisher publisher;

    @GetMapping(path = "/test")
    public String getTest() {
        return "OK";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createNaming(@RequestBody String message) {
        NotificationEvent n = new NotificationEvent();
        n.setName(message);
        n.setStructureId(-1L);

        log.info("New Notification");
        publisher.publishEvent(n);
    }

    @GetMapping("/newNotification")
    public SseEmitter getNewNotification() {
        SseEmitter emitter = new SseEmitter(600_000L);
        this.emitters.add(emitter);

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            this.emitters.remove(emitter);
        });

        return emitter;
    }

    @EventListener
    public void onNotification(NotificationEvent notification) throws IOException {
        log.info("Event Received:" + notification);
        List<SseEmitter> deadEmitters = new ArrayList<>();

        this.emitters.forEach(emitter -> {
            SseEmitter.SseEventBuilder sseEventBuilder =
                    SseEmitter.event().id(UUID.randomUUID().toString())
                              .name("RandomEvent").data(notification);

            try {
                //emitter.send(notification);
                emitter.send(sseEventBuilder);
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        });
        this.emitters.remove(deadEmitters);

        //this.emitter.send(notification);
    }

    @GetMapping("/emitter")
    public SseEmitter eventEmitter() throws IOException {
        SseEmitter.SseEventBuilder sseEventBuilder =
                SseEmitter.event().id(UUID.randomUUID().toString())
                          .name("RandomEvent").data("V:" + Math.random());
        emitter.send(sseEventBuilder);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                for (int i = 0; i < 400; i++) {
                    emitter.send("message" + i);
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                emitter.completeWithError(e);
            } finally {
                emitter.complete();
            }
        });
        executor.shutdown();
        return emitter;
    }
}
