import { useState } from 'react';

// Icons using SVG directly to avoid adding massive icon library dependencies
const HomeIcon = ({ filled }: { filled?: boolean }) => (
  <svg fill={filled ? "currentColor" : "none"} viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
  </svg>
);

const SearchIcon = () => (
  <svg fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
  </svg>
);

const PlusIcon = () => (
  <svg fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" style={{ width: 24, height: 24 }}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
  </svg>
);

const BellIcon = () => (
  <svg fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
  </svg>
);

const UserIcon = () => (
  <svg fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
  </svg>
);

const HeartIcon = ({ filled }: { filled?: boolean }) => (
  <svg fill={filled ? "#ef4444" : "none"} viewBox="0 0 24 24" strokeWidth={filled ? 0 : 1.5} stroke={filled ? "#ef4444" : "currentColor"}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />
  </svg>
);

const ChatIcon = () => (
  <svg fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.436 3 11.996c0 2.29.98 4.346 2.59 5.824v3.43c0 .263.29.414.507.26l3.18-2.27c1.22.463 2.57.71 3.96.71z" />
  </svg>
);

const ShareIcon = () => (
  <svg fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" d="M7.217 10.907a2.25 2.25 0 100 2.186m0-2.186c.18.324.283.696.283 1.093s-.103.77-.283 1.093m0-2.186l9.566-5.314m-9.566 7.5l9.566 5.314m0 0a2.25 2.25 0 103.935 2.186 2.25 2.25 0 00-3.935-2.186zm0-12.814a2.25 2.25 0 103.933-2.185 2.25 2.25 0 00-3.933 2.185z" />
  </svg>
);

function App() {
  const [activeTab, setActiveTab] = useState('home');
  const [likes, setLikes] = useState<Record<number, boolean>>({});

  const toggleLike = (id: number) => {
    setLikes(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const feedData = [
    {
      id: 1,
      name: "Alex Designer",
      time: "2h ago",
      text: "Just finished the new mobile UI kit! What do you guys think? The glassmorphism is really bringing it together. ✨",
      gradient: "linear-gradient(135deg, #a855f7 0%, #3b82f6 100%)",
      likes: 124,
      comments: 14
    },
    {
      id: 2,
      name: "Jamie Developer",
      time: "5h ago",
      text: "React + Vite is truly an amazing combo! The startup time is completely unparalleled.",
      gradient: "linear-gradient(135deg, #ec4899 0%, #f43f5e 100%)",
      likes: 89,
      comments: 7
    },
    {
      id: 3,
      name: "Sam Artist",
      time: "12h ago",
      text: "Exploring some new color palettes today for personal branding projects.",
      gradient: "linear-gradient(135deg, #10b981 0%, #059669 100%)",
      likes: 230,
      comments: 32
    }
  ];

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <h1 className="app-title">Key Feed</h1>
        <div className="header-icon">
          <BellIcon />
        </div>
      </header>

      {/* Main Scrolled Content Area */}
      <main className="app-content">
        {feedData.map((item) => (
          <article className="feed-card" key={item.id}>
            <div className="feed-header">
              <div className="avatar"></div>
              <div className="user-info">
                <span className="user-name">{item.name}</span>
                <span className="time-stamp">{item.time}</span>
              </div>
            </div>
            {/* Visual content placeholder using a nice gradient */}
            <div className="feed-image" style={{ background: item.gradient }}></div>
            <p className="feed-text">{item.text}</p>
            <div className="feed-actions">
              <button 
                className={`action-btn ${likes[item.id] ? 'active' : ''}`}
                onClick={() => toggleLike(item.id)}
              >
                <HeartIcon filled={likes[item.id]} />
                {likes[item.id] ? item.likes + 1 : item.likes}
              </button>
              <button className="action-btn">
                <ChatIcon /> {item.comments}
              </button>
              <button className="action-btn">
                <ShareIcon />
              </button>
            </div>
          </article>
        ))}
      </main>

      {/* Bottom Navigation */}
      <nav className="bottom-nav">
        <div 
          className={`nav-item ${activeTab === 'home' ? 'active' : ''}`}
          onClick={() => setActiveTab('home')}
        >
          <HomeIcon filled={activeTab === 'home'} />
          <span>Home</span>
        </div>
        <div 
          className={`nav-item ${activeTab === 'search' ? 'active' : ''}`}
          onClick={() => setActiveTab('search')}
        >
          <SearchIcon />
          <span>Search</span>
        </div>
        
        <div className="nav-item">
          <div className="fab">
            <PlusIcon />
          </div>
        </div>
        
        <div 
          className={`nav-item ${activeTab === 'notifications' ? 'active' : ''}`}
          onClick={() => setActiveTab('notifications')}
        >
          <BellIcon />
          <span>Alerts</span>
        </div>
        <div 
          className={`nav-item ${activeTab === 'profile' ? 'active' : ''}`}
          onClick={() => setActiveTab('profile')}
        >
          <UserIcon />
          <span>Profile</span>
        </div>
      </nav>
    </div>
  );
}

export default App;
