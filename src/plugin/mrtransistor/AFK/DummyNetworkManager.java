package plugin.mrtransistor.AFK;

import io.netty.channel.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import net.minecraft.server.v1_16_R3.EnumProtocolDirection;
import net.minecraft.server.v1_16_R3.NetworkManager;
import net.minecraft.server.v1_16_R3.Packet;

import java.net.SocketAddress;

public class DummyNetworkManager extends NetworkManager {

    public DummyNetworkManager() {
        super(EnumProtocolDirection.SERVERBOUND);

        this.channel = new EmptyNettyChannel(null);
        this.socketAddress = new SocketAddress() {
        };
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    public void sendPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> genericfuturelistener) {
        /* do nothing */
    }

    @Override
    public void handleDisconnection() {
        /* do nothing */
    }

    public class EmptyNettyChannel extends AbstractChannel {
        private final ChannelConfig config = new DefaultChannelConfig(this);

        public EmptyNettyChannel(Channel parent) {
            super(parent);
        }

        @Override
        public ChannelConfig config() {
            config.setAutoRead(true);
            return config;
        }

        @Override
        protected void doBeginRead() {
        }

        @Override
        protected void doBind(SocketAddress address) {
        }

        @Override
        protected void doClose() {
        }

        @Override
        protected void doDisconnect() {
        }

        @Override
        protected void doWrite(ChannelOutboundBuffer buffer) {
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        protected boolean isCompatible(EventLoop loop) {
            return true;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        protected SocketAddress localAddress0() {
            return null;
        }

        @Override
        public ChannelMetadata metadata() {
            return new ChannelMetadata(true);
        }

        @Override
        protected AbstractUnsafe newUnsafe() {
            return null;
        }

        @Override
        protected SocketAddress remoteAddress0() {
            return null;
        }
    }
}