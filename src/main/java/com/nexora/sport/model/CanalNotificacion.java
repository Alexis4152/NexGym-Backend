package com.nexora.sport.model;

/**
 * Canales de entrega de una notificacion. Solo EMAIL esta implementado por ahora
 * (la notificacion "interna" no es un canal aparte: es la propia fila de
 * {@link Notificacion}, siempre visible en la campana si internoVisible=true).
 * WHATSAPP/PUSH/SMS quedan declarados para que agregar un canal nuevo en el futuro
 * sea insertar un valor aqui y un dispatcher, no rediseñar el modelo.
 */
public enum CanalNotificacion {
    EMAIL,
    WHATSAPP,
    PUSH,
    SMS
}
