package com.pszymczyk.consul.infrastructure.client

import com.pszymczyk.consul.Service
import groovy.transform.PackageScope
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

class SimpleConsulClient {

    private static final String NO_LEADER_ELECTED_RESPONSE = ""

    private final RESTClient http

    @PackageScope
    SimpleConsulClient(RESTClient http) {
        this.http = http
    }

    boolean isLeaderElected() {
        HttpResponseDecorator response = http.get(path: '/v1/status/leader')

        response.getData() != NO_LEADER_ELECTED_RESPONSE
    }

    Collection getRegisteredNodes() {
        HttpResponseDecorator response = http.get(path: '/v1/catalog/nodes')

        response.getData()
    }

    Collection<String> getServicesIds() {
        HttpResponseDecorator response = http.get(path: '/v1/agent/services')

        response.getData()
                .keySet()
                .findAll({ it -> it != 'consul' })
    }

    void register(Service service) {
        http.put(path: "/v1/agent/service/register", contentType: ContentType.JSON, body: getBody(service))
    }

    private static def getBody(Service service) {
        def body = [Name: service.name]
        if (service.address != null) {
            body.Address = service.address
        }
        if (service.port != null) {
            body.Port = service.port
        }
        body
    }

    void deregister(String id) {
        http.put(path: "/v1/agent/service/deregister/$id", contentType: ContentType.ANY)
    }

    void clearKvStore() {
        http.delete(path: "/v1/kv/", query: [recurse: true], contentType: ContentType.ANY)
    }

    void destroyActiveSessions() {
        HttpResponseDecorator response = http.get(path: "/v1/session/list")

        response.getData().each {
            def id = it.ID
            http.put(path: "/v1/session/destroy/$id", contentType: ContentType.ANY)
        }
    }

    void deregisterAllChecks() {
        HttpResponseDecorator response = http.get(path: "/v1/agent/checks")

        response.getData().each {
            def id = it.key

            http.put(path: "/v1/agent/check/deregister/$id")
        }
    }
}
