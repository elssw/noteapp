const functions = require("firebase-functions");
const nodemailer = require("nodemailer");

// 使用 Firebase config 設定 Gmail
const gmailEmail = functions.config().gmail.email;
const gmailPass = functions.config().gmail.pass;

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: gmailEmail,
    pass: gmailPass,
  },
});

exports.sendGroupInvite = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).send("只允許 POST 請求");
  }

  const { email, groupName, link } = req.body;

  const mailOptions = {
    from: `群組邀請 <${gmailEmail}>`,
    to: email,
    subject: `邀請您加入群組「${groupName}」`,
    text: `您好，您被邀請加入我們的群組「${groupName}」。請點擊以下連結以加入：${link}`,
  };

  try {
    await transporter.sendMail(mailOptions);
    return res.status(200).send({ success: true, message: "邀請信已寄出" });
  } catch (error) {
    console.error("寄送失敗", error);
    return res.status(500).send({ success: false, message: "寄送失敗" });
  }
});
