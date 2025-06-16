import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import videoService from '../services/videoService';
import VideoGrid from '../components/Video/VideoGrid';
import Pagination from '../components/Video/Pagination';
import Header from '../components/Layout/Header';

const VideosPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [paginationData, setPaginationData] = useState({
    currentPage: 0,
    totalPages: 0,
    totalElements: 0,
    pageSize: 12
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [searchInputValue, setSearchInputValue] = useState('');
  const [activeTab, setActiveTab] = useState('all'); // 'all', 'popular', 'recent'

  // Get initial values from URL params
  useEffect(() => {
    const page = parseInt(searchParams.get('page')) || 0;
    const query = searchParams.get('q') || '';
    const tab = searchParams.get('tab') || 'all';
    
    setSearchQuery(query);
    setSearchInputValue(query);
    setActiveTab(tab);
    setPaginationData(prev => ({ ...prev, currentPage: page }));
  }, [searchParams]);

  const fetchVideos = useCallback(async (page = 0, query = '', tab = 'all') => {
    setLoading(true);
    setError(null);

    try {
      let response;
      
      if (query.trim()) {
        response = await videoService.searchVideos(query.trim(), page, 12);
      } else {
        switch (tab) {
          case 'popular':
            response = await videoService.getPopularVideos(page, 12);
            break;
          case 'recent':
            response = await videoService.getRecentVideos(page, 12);
            break;
          default:
            response = await videoService.getAllVideos(page, 12);
        }
      }

      setVideos(response.content || []);
      setPaginationData({
        currentPage: response.pageable?.pageNumber || 0,
        totalPages: response.totalPages || 0,
        totalElements: response.totalElements || 0,
        pageSize: response.pageable?.pageSize || 12
      });
    } catch (err) {
      setError(err.message);
      setVideos([]);
      setPaginationData({
        currentPage: 0,
        totalPages: 0,
        totalElements: 0,
        pageSize: 12
      });
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch videos when parameters change
  useEffect(() => {
    fetchVideos(paginationData.currentPage, searchQuery, activeTab);
  }, [fetchVideos, paginationData.currentPage, searchQuery, activeTab]);

  const handlePageChange = (newPage) => {
    const params = new URLSearchParams(searchParams);
    params.set('page', newPage.toString());
    setSearchParams(params);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    
    if (searchInputValue.trim()) {
      params.set('q', searchInputValue.trim());
    }
    params.set('page', '0');
    params.set('tab', 'all');
    
    setSearchParams(params);
  };

  const handleTabChange = (newTab) => {
    const params = new URLSearchParams();
    params.set('tab', newTab);
    params.set('page', '0');
    
    if (searchQuery) {
      params.set('q', searchQuery);
    }
    
    setSearchParams(params);
  };

  const clearSearch = () => {
    setSearchInputValue('');
    const params = new URLSearchParams();
    params.set('tab', activeTab);
    params.set('page', '0');
    setSearchParams(params);
  };

  const getPageTitle = () => {
    if (searchQuery) {
      return `Search results for "${searchQuery}"`;
    }
    
    switch (activeTab) {
      case 'popular':
        return 'Popular Videos';
      case 'recent':
        return 'Recently Watched';
      default:
        return 'All Videos';
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: '#fafafa'
    }}>
      <Header />
      
      <div style={{
        maxWidth: '1400px',
        margin: '0 auto',
        backgroundColor: 'white',
        minHeight: 'calc(100vh - 80px)',
        boxShadow: '0 0 20px rgba(0, 0, 0, 0.1)'
      }}>
        {/* Header section */}
        <div style={{
          padding: '30px 20px 20px',
          borderBottom: '1px solid #e0e0e0'
        }}>
          <h1 style={{
            margin: '0 0 20px 0',
            fontSize: '28px',
            fontWeight: '700',
            color: '#1a1a1a'
          }}>
            {getPageTitle()}
          </h1>

          {/* Search bar */}
          <form onSubmit={handleSearch} style={{
            display: 'flex',
            gap: '12px',
            marginBottom: '20px',
            maxWidth: '600px'
          }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <input
                type="text"
                placeholder="Search videos..."
                value={searchInputValue}
                onChange={(e) => setSearchInputValue(e.target.value)}
                style={{
                  width: '100%',
                  padding: '12px 16px',
                  paddingRight: searchInputValue ? '40px' : '16px',
                  border: '2px solid #e0e0e0',
                  borderRadius: '8px',
                  fontSize: '14px',
                  outline: 'none',
                  transition: 'border-color 0.2s ease'
                }}
                onFocus={(e) => {
                  e.target.style.borderColor = '#d32f2f';
                }}
                onBlur={(e) => {
                  e.target.style.borderColor = '#e0e0e0';
                }}
              />
              {searchInputValue && (
                <button
                  type="button"
                  onClick={clearSearch}
                  style={{
                    position: 'absolute',
                    right: '8px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    fontSize: '18px',
                    cursor: 'pointer',
                    color: '#999',
                    padding: '4px'
                  }}
                >
                  ×
                </button>
              )}
            </div>
            <button
              type="submit"
              style={{
                padding: '12px 24px',
                backgroundColor: '#d32f2f',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: '500',
                cursor: 'pointer',
                transition: 'background-color 0.2s ease'
              }}
              onMouseEnter={(e) => {
                e.target.style.backgroundColor = '#b71c1c';
              }}
              onMouseLeave={(e) => {
                e.target.style.backgroundColor = '#d32f2f';
              }}
            >
              Search
            </button>
          </form>

          {/* Tabs */}
          {!searchQuery && (
            <div style={{
              display: 'flex',
              gap: '4px'
            }}>
              {[
                { id: 'all', label: 'All Videos' },
                { id: 'popular', label: 'Popular' },
                { id: 'recent', label: 'Recently Watched' }
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => handleTabChange(tab.id)}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: activeTab === tab.id ? '#d32f2f' : 'transparent',
                    color: activeTab === tab.id ? 'white' : '#666',
                    border: activeTab === tab.id ? 'none' : '1px solid #e0e0e0',
                    borderRadius: '6px',
                    fontSize: '14px',
                    fontWeight: '500',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease'
                  }}
                  onMouseEnter={(e) => {
                    if (activeTab !== tab.id) {
                      e.target.style.backgroundColor = '#f5f5f5';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (activeTab !== tab.id) {
                      e.target.style.backgroundColor = 'transparent';
                    }
                  }}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          )}

          {/* Results summary */}
          {!loading && paginationData.totalElements > 0 && (
            <div style={{
              marginTop: '16px',
              fontSize: '14px',
              color: '#666'
            }}>
              Found {paginationData.totalElements.toLocaleString()} video{paginationData.totalElements !== 1 ? 's' : ''}
            </div>
          )}
        </div>

        {/* Content */}
        <VideoGrid 
          videos={videos} 
          loading={loading} 
          error={error} 
        />

        {/* Pagination */}
        {!loading && !error && paginationData.totalPages > 1 && (
          <Pagination
            currentPage={paginationData.currentPage}
            totalPages={paginationData.totalPages}
            totalElements={paginationData.totalElements}
            pageSize={paginationData.pageSize}
            onPageChange={handlePageChange}
            loading={loading}
          />
        )}
      </div>
    </div>
  );
};

export default VideosPage; 