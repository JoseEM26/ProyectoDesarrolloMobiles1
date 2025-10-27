// src/app/pipes/generate-pages.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'generatePages',
  standalone: true
})
export class GeneratePagesPipe implements PipeTransform {
  transform(totalPages: number, currentPage: number | null): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    const validTotalPages = totalPages > 0 ? totalPages : 1;
    const validCurrentPage = currentPage && currentPage > 0 ? currentPage : 1;

    let startPage: number, endPage: number;

    if (validTotalPages <= maxPagesToShow) {
      startPage = 1;
      endPage = validTotalPages;
    } else {
      const maxPagesBeforeCurrent = Math.floor(maxPagesToShow / 2);
      const maxPagesAfterCurrent = Math.ceil(maxPagesToShow / 2) - 1;

      startPage = Math.max(1, validCurrentPage - maxPagesBeforeCurrent);
      endPage = Math.min(validTotalPages, validCurrentPage + maxPagesAfterCurrent);

      if (endPage - startPage < maxPagesToShow - 1) {
        if (startPage === 1) {
          endPage = startPage + maxPagesToShow - 1;
        } else if (endPage === validTotalPages) {
          startPage = endPage - maxPagesToShow + 1;
        }
      }
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    console.log('Páginas generadas:', pages); // Log 16
    return pages;
  }
}
