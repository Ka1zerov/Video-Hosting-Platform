import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Basic CSS reset and global styles
const globalStyles = `
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }
  
  body {
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    background-color: #fafafa;
    color: #333;
    line-height: 1.6;
  }
  
  a {
    color: #d32f2f;
    text-decoration: none;
    transition: color 0.2s;
  }
  
  a:hover {
    color: #b71c1c;
  }
  
  button:focus,
  input:focus,
  textarea:focus {
    outline: 2px solid #d32f2f;
    outline-offset: 2px;
  }
  
  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }
  
  .text-center {
    text-align: center;
  }
  
  .text-red {
    color: #d32f2f;
  }
  
  .text-gray {
    color: #666;
  }
  
  .bg-white {
    background-color: white;
  }
  
  .shadow {
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  }
  
  .rounded {
    border-radius: 8px;
  }
  
  .p-4 {
    padding: 20px;
  }
  
  .mb-4 {
    margin-bottom: 20px;
  }
  
  .mt-4 {
    margin-top: 20px;
  }
`;

// Inject global styles
const styleSheet = document.createElement('style');
styleSheet.textContent = globalStyles;
document.head.appendChild(styleSheet);

// Create root and render app
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />); 