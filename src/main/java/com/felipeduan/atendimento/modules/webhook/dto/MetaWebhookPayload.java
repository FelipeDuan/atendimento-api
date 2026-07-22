package com.felipeduan.atendimento.modules.webhook.dto;

import java.util.List;

public record MetaWebhookPayload(List<Entry> entry) {

  public record Entry(List<Change> changes) {}

  public record Change(Value value) {}

  public record Value(Metadata metadata, List<Contact> contacts, List<Message> messages) {}

  public record Metadata(String phone_number_id) {}

  public record Contact(String wa_id, Profile profile) {}

  public record Profile(String name) {}

  public record Message(
      String id, String from, String type, Text text, Image image, Document document) {}

  public record Text(String body) {}

  public record Image(String id, String caption, String mime_type) {}

  public record Document(String id, String caption, String filename, String mime_type) {}
}
