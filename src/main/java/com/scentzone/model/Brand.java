package com.scentzone.model;

public record Brand(int id, String name) {
    @Override public String toString() { return name; }
}
