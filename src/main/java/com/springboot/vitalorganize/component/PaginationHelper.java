package com.springboot.vitalorganize.component;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaginationHelper {
    public Pageable createPageable(int page, int size, String sortBy, Sort.Direction direction) {
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
    public Pageable createSingleItemPageable(String sortBy, Sort.Direction direction) {
        return PageRequest.of(0, 1, Sort.by(direction, sortBy));
    }

    public <T> T getFirstElement(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }
}
