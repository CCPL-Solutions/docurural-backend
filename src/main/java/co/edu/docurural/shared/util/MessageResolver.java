package co.edu.docurural.shared.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolvedor centralizado de mensajes internacionalizados.
 *
 * <p>Proporciona un punto único para resolver mensaje desde {@link MessageSource}
 * respetando el locale del contexto actual ({@link LocaleContextHolder}).
 * Esto elimina la duplicación de código en servicios, controladores y componentes
 * de seguridad que de otro modo contendrían métodos {@code resolve(...)} repetidos.
 */
@Component
@RequiredArgsConstructor
public class MessageResolver {

    private final MessageSource messageSource;

    /**
     * Resuelve un mensaje internacionalizado según el key y el locale actual.
     *
     * @param key  clave del mensaje en {@code messages.properties}.
     * @param args argumentos para interpolar en el mensaje (opcional).
     * @return mensaje localizado.
     */
    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}

