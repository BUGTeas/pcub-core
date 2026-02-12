package org.pcub.core.geyser.listener;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.*;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.session.GeyserSession;


import static org.pcub.core.common.PCUBCore.logger;

public class EventListener4Geyser implements EventRegistrar {
    public EventListener4Geyser(GeyserApi geyserApi) {
        geyserApi.eventBus().subscribe(this, SessionJoinEvent.class, this::onSessionJoin);
    }

    public void onSessionJoin(SessionJoinEvent event) {
        GeyserSession session = (GeyserSession) event.connection();
        new DownstreamListener4Geyser(session);
    }
}
