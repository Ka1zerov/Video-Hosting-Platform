import React from 'react';

const Pagination = ({ 
  currentPage, 
  totalPages, 
  totalElements, 
  pageSize, 
  onPageChange,
  loading = false 
}) => {
  if (totalPages <= 1) {
    return null;
  }

  const getVisiblePages = () => {
    const maxVisible = 5;
    const pages = [];
    
    if (totalPages <= maxVisible) {
      // Show all pages if total is small
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Show pages around current page
      const start = Math.max(0, currentPage - 2);
      const end = Math.min(totalPages - 1, currentPage + 2);
      
      // Add first page
      if (start > 0) {
        pages.push(0);
        if (start > 1) {
          pages.push('...');
        }
      }
      
      // Add visible range
      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
      
      // Add last page
      if (end < totalPages - 1) {
        if (end < totalPages - 2) {
          pages.push('...');
        }
        pages.push(totalPages - 1);
      }
    }
    
    return pages;
  };

  const handlePageClick = (page) => {
    if (page !== '...' && page !== currentPage && !loading) {
      onPageChange(page);
    }
  };

  const visiblePages = getVisiblePages();
  const startItem = currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: '16px',
      padding: '20px',
      backgroundColor: 'white',
      borderTop: '1px solid #e0e0e0'
    }}>
      {/* Results info */}
      <div style={{
        fontSize: '14px',
        color: '#666',
        textAlign: 'center'
      }}>
        Showing {startItem}-{endItem} of {totalElements.toLocaleString()} videos
      </div>

      {/* Pagination controls */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px'
      }}>
        {/* Previous button */}
        <button
          onClick={() => handlePageClick(currentPage - 1)}
          disabled={currentPage === 0 || loading}
          style={{
            padding: '8px 12px',
            backgroundColor: currentPage === 0 || loading ? '#f5f5f5' : '#fff',
            border: '1px solid #e0e0e0',
            borderRadius: '6px',
            cursor: currentPage === 0 || loading ? 'not-allowed' : 'pointer',
            color: currentPage === 0 || loading ? '#ccc' : '#666',
            fontSize: '14px',
            fontWeight: '500',
            transition: 'all 0.2s ease'
          }}
          onMouseEnter={(e) => {
            if (currentPage !== 0 && !loading) {
              e.target.style.backgroundColor = '#f0f0f0';
            }
          }}
          onMouseLeave={(e) => {
            if (currentPage !== 0 && !loading) {
              e.target.style.backgroundColor = '#fff';
            }
          }}
        >
          ← Previous
        </button>

        {/* Page numbers */}
        <div style={{
          display: 'flex',
          gap: '4px'
        }}>
          {visiblePages.map((page, index) => (
            <button
              key={index}
              onClick={() => handlePageClick(page)}
              disabled={loading}
              style={{
                padding: '8px 12px',
                backgroundColor: page === currentPage ? '#d32f2f' : 
                                page === '...' ? 'transparent' : '#fff',
                border: page === '...' ? 'none' : '1px solid #e0e0e0',
                borderRadius: '6px',
                cursor: page === '...' || loading ? 'default' : 'pointer',
                color: page === currentPage ? 'white' : 
                       page === '...' ? '#999' : '#666',
                fontSize: '14px',
                fontWeight: page === currentPage ? '600' : '500',
                minWidth: '40px',
                transition: 'all 0.2s ease'
              }}
              onMouseEnter={(e) => {
                if (page !== '...' && page !== currentPage && !loading) {
                  e.target.style.backgroundColor = '#f0f0f0';
                }
              }}
              onMouseLeave={(e) => {
                if (page !== '...' && page !== currentPage && !loading) {
                  e.target.style.backgroundColor = '#fff';
                }
              }}
            >
              {page === '...' ? '...' : page + 1}
            </button>
          ))}
        </div>

        {/* Next button */}
        <button
          onClick={() => handlePageClick(currentPage + 1)}
          disabled={currentPage >= totalPages - 1 || loading}
          style={{
            padding: '8px 12px',
            backgroundColor: currentPage >= totalPages - 1 || loading ? '#f5f5f5' : '#fff',
            border: '1px solid #e0e0e0',
            borderRadius: '6px',
            cursor: currentPage >= totalPages - 1 || loading ? 'not-allowed' : 'pointer',
            color: currentPage >= totalPages - 1 || loading ? '#ccc' : '#666',
            fontSize: '14px',
            fontWeight: '500',
            transition: 'all 0.2s ease'
          }}
          onMouseEnter={(e) => {
            if (currentPage < totalPages - 1 && !loading) {
              e.target.style.backgroundColor = '#f0f0f0';
            }
          }}
          onMouseLeave={(e) => {
            if (currentPage < totalPages - 1 && !loading) {
              e.target.style.backgroundColor = '#fff';
            }
          }}
        >
          Next →
        </button>
      </div>

      {/* Loading indicator */}
      {loading && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          fontSize: '14px',
          color: '#666'
        }}>
          <div style={{
            width: '16px',
            height: '16px',
            border: '2px solid #e0e0e0',
            borderTop: '2px solid #d32f2f',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite'
          }} />
          Loading...
        </div>
      )}

      <style>
        {`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}
      </style>
    </div>
  );
};

export default Pagination; 