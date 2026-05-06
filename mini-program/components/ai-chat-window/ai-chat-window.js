Component({
  properties: {
    shopName: {
      type: String,
      value: '苍穹外卖'
    }
  },

  data: {
    isOpen: false,
    messages: [{
      role: 'ai',
      content: '您好呀！我是您的店小二 🍱\n今天想吃点什么？我可以帮您推荐特色菜，或者帮您查询订单进度哦~'
    }],
    inputValue: '',
    lastMessageId: '',
    isThinking: false,
    quickReplies: ['推荐特色菜', '查询订单', '店铺营业时间', '热销菜品'],
    scrollHeight: 400  // 默认值，会在 onOpen 时重新计算
  },

  lifetimes: {
    attached() {
      const sys = wx.getSystemInfoSync();
      // 聊天窗口占 75vh，减去头部、输入栏等固定区域后为可滚动高度
      const chatWindowHeight = sys.windowHeight * 0.75;
      const fixedHeight = 180; // 头部 + 输入栏 + 安全区，单位 px
      this.setData({ scrollHeight: chatWindowHeight - fixedHeight });
    }
  },

  methods: {
    onOpen() {
      // 每次打开重新计算高度（适配不同场景）
      const sys = wx.getSystemInfoSync();
      const chatWindowHeight = sys.windowHeight * 0.75;
      const fixedHeight = 180;
      this.setData({
        isOpen: true,
        scrollHeight: chatWindowHeight - fixedHeight
      });
    },

    // 关闭聊天窗口
    onClose() {
      this.setData({ isOpen: false });
    },

    // 阻止冒泡(防止点击窗口内部时关闭)
    onStopPropagation() {},

    // 输入事件
    onInput(e) {
      this.setData({ inputValue: e.detail.value });
    },

    // 快捷回复
    onQuickReply(e) {
      const text = e.currentTarget.dataset.text;
      this.setData({ inputValue: text });
      this.onSend();
    },

    // 发送消息
    onSend() {
      const text = this.data.inputValue.trim();
      if (!text || this.data.isThinking) return;

      let userToken = '';
      try {
        const pages = getCurrentPages();
        const page = pages[pages.length - 1];
        if (page && page.$vm && page.$vm.$store && page.$vm.$store.state) {
          userToken = page.$vm.$store.state.token || '';
        }
      } catch (e) {}
      if (!userToken) {
        try {
          const app = getApp();
          if (app && app.$store && app.$store.state) {
            userToken = app.$store.state.token || '';
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
              title: res.data.msg || '小二暂时无法回复，请稍后再试',
              icon: 'none'
            });
          }
        },
        fail: (err) => {
          console.error('AI请求失败：', err);
          wx.showToast({
            title: '网络连接失败，请检查网络',
            icon: 'none'
          });
        },
        complete: () => {
          this.setData({ isThinking: false });
        }
      });
    },

    // 滚动到底部
    scrollToBottom() {
      this.setData({
        lastMessageId: `msg-${this.data.messages.length - 1}`
      });
    }
  }
});
