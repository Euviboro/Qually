package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Pagination wrapper for the Results table endpoint.
 *
 * <p>Carries both the page content and the metadata the frontend needs to
 * render the numbered pagination control: total element count, total pages,
 * and the current page index (0-based on the server, displayed as 1-based
 * in the UI).</p>
 */
@Getter
@Builder
public class PagedResultsResponseDTO {

    /** The result rows for this page. */
    private List<ResultsTableRowDTO> content;

    /** Total number of rows matching the current filters across all pages. */
    private long totalElements;

    /** Total number of pages at the requested page size. */
    private int totalPages;

    /** Zero-based index of the current page. */
    private int currentPage;

    /** Number of rows per page (as requested by the client). */
    private int pageSize;
}
