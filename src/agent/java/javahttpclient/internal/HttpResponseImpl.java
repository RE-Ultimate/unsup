/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javahttpclient.internal;

import java.net.URI;
import java.util.Optional;
import javax.net.ssl.SSLSession;

import javahttpclient.HttpClient;
import javahttpclient.HttpHeaders;
import javahttpclient.HttpRequest;
import javahttpclient.HttpResponse;

/**
 * The implementation class for HttpResponse
 */
class HttpResponseImpl<T> implements HttpResponse<T> {

    final int responseCode;
    private final String connectionLabel;
    final HttpRequest initialRequest;
    final Optional<HttpResponse<T>> previousResponse;
    final HttpHeaders headers;
    final Optional<SSLSession> sslSession;
    final URI uri;
    final HttpClient.Version version;
    final T body;

    public HttpResponseImpl(HttpRequest initialRequest,
                            Response response,
                            HttpResponse<T> previousResponse,
                            T body,
                            Exchange<T> exch) {
        this.responseCode = response.statusCode();
        this.connectionLabel = connectionLabel(exch).orElse(null);
        this.initialRequest = initialRequest;
        this.previousResponse = Optional.ofNullable(previousResponse);
        this.headers = response.headers();
        //this.trailers = trailers;
        this.sslSession = Optional.ofNullable(response.getSSLSession());
        this.uri = response.request().uri();
        this.version = response.version();
        this.body = body;
    }

    private static Optional<String> connectionLabel(Exchange<?> exchange) {
        return Optional.ofNullable(exchange)
                .map(e -> e.exchImpl)
                .map(ExchangeImpl::connection)
                .map(HttpConnection::label);
    }

    @Override
    public int statusCode() {
        return responseCode;
    }

    @Override
    public Optional<String> connectionLabel() {
        return Optional.ofNullable(connectionLabel);
    }

    @Override
    public HttpRequest request() {
        return initialRequest;
    }

    @Override
    public Optional<HttpResponse<T>> previousResponse() {
        return previousResponse;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public T body() {
        return body;
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return sslSession;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public HttpClient.Version version() {
        return version;
    }
    // keepalive flag determines whether connection is closed or kept alive
    // by reading/skipping data

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String method = request().method();
        URI uri = request().uri();
        String uristring = uri == null ? "" : uri.toString();
        sb.append('(')
          .append(method)
          .append(" ")
          .append(uristring)
          .append(") ")
          .append(statusCode());
        return sb.toString();
    }
}
