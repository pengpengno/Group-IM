import React from 'react';
import './Loading.css';

interface LoadingProps {
    fullScreen?: boolean;
    text?: string;
}

const Loading: React.FC<LoadingProps> = ({ fullScreen, text = '核对信息中...' }) => {
    return (
        <div className={`loading-container ${fullScreen ? 'full-screen' : ''}`}>
            <div className="loading-content">
                <div className="premium-spinner">
                    <div className="spinner-inner"></div>
                    <div className="spinner-outer"></div>
                    <div className="spinner-center"></div>
                </div>
                {text && <p className="loading-text">{text}</p>}
            </div>
        </div>
    );
};

export default Loading;
