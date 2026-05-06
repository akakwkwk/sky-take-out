Page({
  data: {
    messages: [{
      role: 'ai',
      content: '您好呀！我是您的智能店小二 🍱\n\n今天想吃点什么？我帮您推荐特色菜，也可以帮您查询订单进度哦~'
    }],
    inputValue: '',
    lastMessageId: '',
    isThinking: false,
    quickReplies: ['推荐特色菜', '查询热销菜品', '店铺营业时间', '我的订单'],
    scrollHeight: 500
  },

  onLoad() {
    const sys = wx.getSystemInfoSync();
    // 页面总高度减去输入栏和快捷回复等固定区域
    const fixedHeight = 170; // 输入栏 + 快捷回复 + nav bar
    this.setData({ scrollHeight: sys.windowHeight - fixedHeight });
  },

  onInput(e) {
    this.setData({ inputValue: e.detail.value });
  },

  // 快捷回复
  onQuickReply(e) {
    const text = e.currentTarget.dataset.text;
    this.setData({ inputValue: text });
    this.onSend();
  },

  onSend() {
    const text = this.data.inputValue.trim();
    if (!text || this.data.isThinking) return;

    let userToken = '';
    try {
      userToken = this.$store.state.token || '';
    } catch (e) {}
    if (!userToken) {
      try {
        const pages = getCurrentPages();
        const page = pages[pages.length - 1];
        if (page && page.$vm && page.$vm.$store && page.$vm.$store.state) {
          userToken = page.$vm.$store.state.token || '';
        }
      } catch (e) {}
    }
    if (!userToken) {
      userToken = wx.getStorageSync('uni_id_token') || '';
    }

    const newMessages = [...this.data.messages, { role: 'user', content: text }];
    this.setData({
      messages: newMessages,
      inputValue: '',
      isThinking: true
    }, () => {
      this.scrollToBottom();
    });

    wx.request({
      url: 'http://localhost:8080/user/ai/chat',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'authentication': userToken || ''
      },
      data: {
        message: text
      },
      success: (res) => {
        if (res.data.code === 1) {
          const aiMsg = { role: 'ai', content: res.data.data };
          this.setData({
            messages: [...this.data.messages, aiMsg]
          }, () => {
            this.scrollToBottom();
          });
        } else {
          wx.showToast({
            title: res.data.msg || '小二暂时无法回复',
            icon: 'none'
          });
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络连接失败，请检查网络',
          icon: 'none'
        });
        console.error('AI请求错误：', err);
      },
      complete: () => {
        this.setData({ isThinking: false });
      }
    });
  },

  scrollToBottom() {
    this.setData({
      lastMessageId: `msg-${this.data.messages.length - 1}`
    });
  }
});
