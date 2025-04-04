package kr.cocoh.api.model.auth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import kr.cocoh.api.model.auth.enums.Provider;

@Converter
public class ProviderConverter implements AttributeConverter<Provider, String> {
    @Override
    public String convertToDatabaseColumn(Provider provider) {
        return provider == null ? null : provider.name().toLowerCase();
    }

    @Override
    public Provider convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Provider.valueOf(dbData.toUpperCase());
    }
}