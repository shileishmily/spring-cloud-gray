package cn.springcloud.gray.client.gateway;

import cn.springcloud.gray.RequestInterceptor;
import cn.springcloud.gray.request.GrayHttpTrackInfo;
import cn.springcloud.gray.request.GrayRequest;
import cn.springcloud.gray.request.HttpGrayTrackRecordDevice;
import cn.springcloud.gray.request.HttpGrayTrackRecordHelper;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.List;

public class GatewayRequestInterceptor implements RequestInterceptor {
    @Override
    public String interceptroType() {
        return "gateway";
    }

    @Override
    public boolean shouldIntercept() {
        return true;
    }

    @Override
    public boolean pre(GrayRequest request) {
        GrayHttpTrackInfo grayTrack = (GrayHttpTrackInfo) request.getGrayTrackInfo();
        if (grayTrack != null) {
            ServerHttpRequest.Builder requestBuilder = (ServerHttpRequest.Builder) request.getAttribute(
                    GrayLoadBalancerClientFilter.GRAY_REQUEST_ATTRIBUTE_GATEWAY_HTTPREQUEST_BUILDER);
            HttpGrayTrackRecordHelper.record(new GatewayHttpGrayTrackRecordDevice(requestBuilder), grayTrack);
        }
        return true;
    }


    public static class GatewayHttpGrayTrackRecordDevice implements HttpGrayTrackRecordDevice {

        private ServerHttpRequest.Builder requestBuilder;

        public GatewayHttpGrayTrackRecordDevice(ServerHttpRequest.Builder requestBuilder) {
            this.requestBuilder = requestBuilder;
        }

        @Override
        public void record(String name, String value) {
            requestBuilder.header(name, value);
        }

        @Override
        public void record(String name, List<String> values) {
            for (String v : values) {
                requestBuilder.header(name, v);
            }
        }
    }

    @Override
    public boolean after(GrayRequest request) {
        return true;
    }
}
