//BOMBERWOMAN - CLIENTE
//Autora: HELOISA HUNGARO PRIMOLAN
//RA: 141026431
//ago-2015

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;
import javax.imageio.*;
import java.util.*;
import java.net.*;

import sun.audio.*;
import java.net.URL;
import javax.sound.sampled.*;


class Cliente extends JFrame implements Runnable
{
	private static Point point = new Point();
	ImageIcon icone = new ImageIcon("figuras/bomba2.png");
	//var do jogo
	boolean jogo=false;
	JLabel lbl, display;
	Image fundo, bloco, blocod;
	Image explode[] = new Image[3];
	Image fogo[] = new Image[4];
	Image avatar[][] = new Image[2][7];
	int posx=-1,posy=-1,advx=-1,advy=-1;
	int passo=50;
	Desenho des = new Desenho();
	int estado = 0; //0 e 1 para dar passos
	int paraonde = -1, paraondeadv = -1; //0 = CIMA, 1 = DIR, 2 = BAIXO, 3 = ESQ, -1 = PARADO
	int destroix[] = new int[30];
	int destroiy[] = new int[30];
	int numbombas=0, numbombasadv=0;
	int bomba[] = new int[4]; //bomba=8: nao iniciada
	int bestado[]  = new int[4];
	int bposx[]  = new int[4];
	int bposy[] = new int[4];
	Random aleatorio = new Random();
	final int PARADO = -1;
	final int  CIMA = 0;
	final int   DIR = 1;
	final int BAIXO = 2;
	final int   ESQ = 3;
	boolean desistiu;
	
	//var conexao
	Socket conexao;
	private DataInputStream input;
	private DataOutputStream output;
	Thread jogoThread;
	
	int jogador = 0,adversario;
	int destroi[]= new int[4];
	int ganhou = -1;
	int ganhouadv = -1;
	
	boolean conexaovalida=false, portavalida=false;
	String portastr, ip;
	int porta;
	
	public void start() //para iniciar conexao
	{
		int i,j,x,y;
		desistiu=false;
		//primeiro exibe instruçoes do jogo
		JOptionPane.showMessageDialog(this,"Voce deve eliminar o adversario. \nJogador 1 eh o vermelho e azul posicionado acima. "+
											"Jogador 2 eh o amarelo e roxo abaixo. \nAs setas movem o personagem. "+
											"Espaco solta uma bomba. \nVoce pode soltar ate 2 bombas ao mesmo tempo. "+
											"Os blocos de tijolinhos sao destrutiveis. Os outros nao. \n"+
											"Pressione ESC caso queira desistir no meio da partida.\nBOM JOGO!","INSTRUCOES",JOptionPane.PLAIN_MESSAGE);
		//agora tenta conectar
		while (!conexaovalida)
		{
			portavalida=false;
			ip=JOptionPane.showInputDialog("Digite o IP do servidor (Exemplo: 127.0.0.1)","127.0.0.1");
			if (ip == null) //se o usuário cancelar digitação, sair do programa
					System.exit(0);
			while (!portavalida)
			{
				portastr=JOptionPane.showInputDialog("Digite a porta de conexao com o servidor (Exemplo: 7890)","7890");
				if (portastr == null) //se o usuário cancelar digitação, sair do programa
					System.exit(0);
				try
				{
					porta=Integer.parseInt(portastr);
					portavalida=true;
				}
				catch (Exception e) //caso a porta não seja válida, mostra mensagem de erro e tenta novamente (while)
				{
					JOptionPane.showMessageDialog(this,"O valor de porta digitado eh invalido. Tente novamente. (Exemplo: 7890)","PORTA INVALIDA",JOptionPane.INFORMATION_MESSAGE);
				}
			}
			try
			{
				conexao = new Socket(ip,porta);
				conexaovalida=true;
			}
			catch (Exception e) //caso a porta ou o ip não seja válido, mostra mensagem de erro e tenta novamente (while)
			{
				JOptionPane.showMessageDialog(this,"Nao foi possivel conectar com os dados de IP e porta digitados. Tente novamente.\n"+e,"FALHA NA CONEXAO COM O SERVIDOR",JOptionPane.INFORMATION_MESSAGE);
			}
		}
		//cliente conectou com servidor!
		jogo=true;
		repaint();
		try
		{
			input = new DataInputStream(conexao.getInputStream());
			output = new DataOutputStream(conexao.getOutputStream());
			display.setText("JOGUE!"); //jogadores conectados, jogue!
			jogador = input.readInt(); //recebe qual jogador é
			lbl.setText("<html>Pressione <font color='red'>ESC</font> para desistir/sair.   |   VOCE: <font color='blue'>JOGADOR "+(jogador+1)+"</font></html>");
			for (i=0;i<4;i++) 
				destroi[i]=-1;		
			if (jogador==0) //inicia posiçoes de jogo do jogador 1
			{
				adversario = 1;
				posx=posy=50;
				advx=advy=450;
				for (i=0;i<30;i++) //cria 30 blocos destrutiveis aleatorios
				{
					do 
					{
						x = aleatorio.nextInt(9)+1;
						y = aleatorio.nextInt(9)+1;
						for (j=0;j<i;j++)
						{
							if ((destroix[j]==50*x) && (destroiy[j]==50*y))
								x=y=2;
						}
					} while (((x%2==0) && (y%2==0)) || ((x<=2) && (y<=2)) || ((x>=8) && (y>=8)));
					destroix[i] = 50*x;
					destroiy[i] = 50*y;
					output.writeInt(destroix[i]); //envia as posiçoes dos blocos
					output.writeInt(destroiy[i]);
					output.flush();
				}				
			}
			else //inicia posiçoes de jogo do jogador 2
			{
				adversario = 0;
				posx=posy=450;
				advx=advy=50;
				for (i=0;i<30;i++) //recebe as posiçoes dos blocos
				{
					destroix[i]=input.readInt();
					destroiy[i]=input.readInt();
				}
			}
			bomba[0]=8;
			bomba[1]=8;
			bomba[2]=8;
			bomba[3]=8;
			repaint();
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this,"Erro ao trocar dados com o servidor\n"+e,"PROBLEMA NA TROCA DE DADOS",JOptionPane.INFORMATION_MESSAGE);
			e.printStackTrace();
			System.exit(1);
		}
		
		jogoThread = new Thread(this); //inicia a thread de troca de dados com o servidor
		jogoThread.start();
	}
	
	public void run()
	{
		while (ganhou==-1) //enquanto nao for fim de jogo, troca dados com o servidor
		{			
			try
			{
				String msg = input.readUTF(); //recebe mensagem do servidor
				if(msg.equals("JOGADA")) //se for jogada, envia posiçoes e outros dados
				{
					try
					{
						if (desistiu==true)
						{
								output.writeInt(1);
								System.exit(0);
						}
						else
							output.writeInt(-1);
						advx = input.readInt();
						advy = input.readInt();
						paraondeadv = input.readInt();
						output.writeInt(posx);
						output.writeInt(posy);
						output.writeInt(paraonde);
						output.flush();
						for (int i=0;i<4;i++) 
						{
						output.writeInt(destroi[i]);
						output.flush();
						destroi[i] = input.readInt();
							if (destroi[i]!=-1)
							{
								destroix[destroi[i]]=-100;
								destroiy[destroi[i]]=-100;
								destroi[i]=-1;
							}
						}
						for (int i=0;i<2;i++) 
						{
							output.writeInt(bomba[i]);
							output.writeInt(bposx[i]);
							output.writeInt(bposy[i]);
							output.flush();
							bomba[i+2]=input.readInt();
							bposx[i+2]=input.readInt();
							bposy[i+2]=input.readInt();
							if (bomba[i+2]==0)
								numbombasadv++;
						}
						output.writeInt(ganhou);
						output.flush();
						repaint();
						//posiciona o jogador adversario
					}
					catch (IOException e)
					{
						JOptionPane.showMessageDialog(this,"Erro ao trocar dados com o servidor\n"+e,"PROBLEMA NA TROCA DE DADOS",JOptionPane.INFORMATION_MESSAGE);
						e.printStackTrace();
						System.exit(1);
					}
				}
				else //se nao for jogada, mostra a mensagem na tela
					display.setText(msg);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(this,"Erro ao trocar dados com o servidor\n"+e,"PROBLEMA NA TROCA DE DADOS",JOptionPane.INFORMATION_MESSAGE);
				e.printStackTrace();
				System.exit(1);
			}
		}
		try 
		{
				display.setText("FIM DE JOGO"); //fim de jogo! exibe a mensagem de venceu/perdeu/empatou e finaliza o programa
				if (ganhou==1)
					JOptionPane.showMessageDialog(this,"Voce ganhou!","FIM DE JOGO",JOptionPane.INFORMATION_MESSAGE);
				else if (ganhouadv==1)
					JOptionPane.showMessageDialog(this,"Voce perdeu!","FIM DE JOGO",JOptionPane.INFORMATION_MESSAGE);
				else 
					JOptionPane.showMessageDialog(this,"Empatou!","FIM DE JOGO",JOptionPane.INFORMATION_MESSAGE);
				conexao.close();
				System.exit(0);	
		}
		catch (IOException e)
		{
			JOptionPane.showMessageDialog(this,"Erro ao trocar dados com o servidor\n"+e,"PROBLEMA NA TROCA DE DADOS",JOptionPane.INFORMATION_MESSAGE);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	//INFORMAÇOES FIXAS DO JOGO ABAIXO
	
	
	public boolean podeandar(int onde) //verifica se é possível o jogador se mover para determinada posiçao
	{
		int i, x=posx, y=posy;
		switch (onde)
		{
			case  CIMA: y-=50;
						break;
			case BAIXO: y+=50;
						break;
			case   DIR: x+=50;
						break;
			case   ESQ: x-=50;
						break;
		}
		if ((x%100==0 && y%100==0) || (x==0) || (y==0) || (x==500) || (y==500) || (x==bposx[0] && y==bposy[0] && bomba[0]<7) || (x==bposx[1] && y==bposy[1] && bomba[1]<7))
			return false;
		for (i=0;i<30;i++)
		{
			if (x==destroix[i] && y==destroiy[i])
				return false;
		}
		if (advx==x && advy==y)
			return false;
		return true;
	}
	
	public boolean podeexplodir(int onde, int b) //verifica se pode haver explosao em uma posiçao: caso nao haja bloco fixo
	{
		int i, x=bposx[b], y=bposy[b];
		switch (onde)
		{
			case  CIMA: y-=50;
						break;
			case BAIXO: y+=50;
						break;
			case   DIR: x+=50;
						break;
			case   ESQ: x-=50;
						break;
		}
		if ((x%100==0 && y%100==0) || (x==0) || (y==0) || (x==500) || (y==500) || 
			(x==bposx[0] && y==bposy[0] && bomba[0]<7) || (x==bposx[1] && y==bposy[1] && bomba[1]<7) ||
			(x==bposx[2] && y==bposy[2] && bomba[2]<7) || (x==bposx[3] && y==bposy[3] && bomba[3]<7))
			return false;
		for (i=0;i<30;i++)
		{
			if (x==destroix[i] && y==destroiy[i])
			{
				destroix[i]=-100;
				destroiy[i]=-100;
				destroi[onde]=i;
				return true;
			}
		}
		return true;
	}
	
	public boolean fimdejogo(int x,int y) //verifica se é fim de jogo! isso ocorre caso algum dos jogadores esteja numa posiçao de explosao
	{
		if (x==advx && y==advy && x==posx && y==posy)
		{
			ganhou = 0;
			ganhouadv = 0;
			return true;
		}
		if (x==posx && y==posy)
		{
			ganhouadv = 1;
			ganhou = 0;
			return true;
		}
		if (x==advx && y==advy)
		{
			ganhou = 1;
			ganhouadv = 0;
			return true;
		}
		return false;
	}
	
	
    class Desenho extends JPanel //paint aqui
	{

		Desenho() 
		{
			try 
			{
				//carrega imagens do jogo
				fundo = ImageIO.read(new File("figuras/fundo.png")); 
				bloco = ImageIO.read(new File("figuras/blocao.png"));
				blocod = ImageIO.read(new File("figuras/bloquinhos.png"));
				avatar[0][0] = ImageIO.read(new File("figuras/b1p.png"));
				avatar[0][1] = ImageIO.read(new File("figuras/b1c1.png"));
				avatar[0][2] = ImageIO.read(new File("figuras/b1c2.png"));
				avatar[0][3] = ImageIO.read(new File("figuras/b1b1.png"));
				avatar[0][4] = ImageIO.read(new File("figuras/b1b2.png"));
				avatar[0][5] = ImageIO.read(new File("figuras/b1l1.png"));
				avatar[0][6] = ImageIO.read(new File("figuras/b1l2.png"));
				avatar[1][0] = ImageIO.read(new File("figuras/b2p.png"));
				avatar[1][1] = ImageIO.read(new File("figuras/b2c1.png"));
				avatar[1][2] = ImageIO.read(new File("figuras/b2c2.png"));
				avatar[1][3] = ImageIO.read(new File("figuras/b2b1.png"));
				avatar[1][4] = ImageIO.read(new File("figuras/b2b2.png"));
				avatar[1][5] = ImageIO.read(new File("figuras/b2l1.png"));
				avatar[1][6] = ImageIO.read(new File("figuras/b2l2.png"));
				explode[0] = ImageIO.read(new File("figuras/bomba1.png"));
				explode[1] = ImageIO.read(new File("figuras/bomba2.png"));
				explode[2] = ImageIO.read(new File("figuras/fogo.png"));
				fogo[0] = ImageIO.read(new File("figuras/fogoc.png"));
				fogo[1] = ImageIO.read(new File("figuras/fogod.png"));
				fogo[2] = ImageIO.read(new File("figuras/fogob.png"));
				fogo[3] = ImageIO.read(new File("figuras/fogoe.png"));
				
			} catch (IOException e) 
			{
				JOptionPane.showMessageDialog(this,"Erro ao carregar imagens!\nVerifique se a pasta 'figuras' esta na mesma pasta que o arquivo java\n"+e,"PROBLEMA AO CARREGAR IMAGENS",JOptionPane.INFORMATION_MESSAGE);
				e.printStackTrace();
				System.exit(1);
			}
		}

		public void paint(Graphics g) 
		{
			int i,j;
			int px = posx;
			int py = posy;
			int ax = advx;
			int ay = advy;
			super.paint(g);
			
			g.drawImage(fundo, 0, 0, getSize().width, getSize().height, this); //fundo do jogo
			
			for (i=0;i<550;i+=50) //desenha os blocos fixos
			{
				g.drawImage(bloco, i, 0, this);
				g.drawImage(bloco, i, 500, this);
				g.drawImage(bloco, 0, i, this);
				g.drawImage(bloco, 500, i, this);
				for (j=0;j<550;j+=50)
				{
					if ((i%100==0) && (j%100==0))
					{
						g.drawImage(bloco, i, j, this);
						if (i!=j)
							g.drawImage(bloco, j, i, this);
					}
				}
			}
			if (!jogo && advx==-1) //se ainda nao houver conexao, para aqui (tambem espera até receber dado do servidor para já posicionar no lugar certo
				return;
			for (i=0;i<30;i++) //desenha os blocos destrutiveis
			{
				g.drawImage(blocod, destroix[i], destroiy[i], this);
			}
			
			for (i=0;i<4;i++) //desenha as bombas caso haja alguma
			{
				if (bomba[i]<7 && bomba[i]>0)
					g.drawImage(explode[bestado[i]], bposx[i], bposy[i], this);
			}
			if (ganhouadv!=1) //se jogador nao perdeu (deixa de aparecer), desenha jogador
			{
				switch (paraonde)
				{
					case PARADO: g.drawImage(avatar[jogador][0], px, py, this);
								 break;
					case CIMA: 	 g.drawImage(avatar[jogador][1+estado], px, py, this);
								 break;
					case BAIXO:  g.drawImage(avatar[jogador][3+estado], px, py, this);
								 break;
					case DIR: 	 g.drawImage(avatar[jogador][5+estado], px, py, this);
								 break;
					case ESQ:	 g.drawImage(avatar[jogador][5+estado], px+50, py, -50, 50, this);
								 break;
				}	
			}
			if (ganhou!=1) //se jogador nao ganhou, desenha adversario
			{
				switch (paraondeadv)
				{
					case PARADO: g.drawImage(avatar[adversario][0], ax, ay, this);
								 break;
					case CIMA: 	 g.drawImage(avatar[adversario][1+estado], ax, ay, this);
								 break;
					case BAIXO:  g.drawImage(avatar[adversario][3+estado], ax, ay, this);
								 break;
					case DIR: 	 g.drawImage(avatar[adversario][5+estado], ax, ay, this);
								 break;
					case ESQ:	 g.drawImage(avatar[adversario][5+estado], ax+50, ay, -50, 50, this);
								 break;
				}
			}
			
			for (i=0;i<4;i++) //desenha explosoes da bomba!
			{
				if (bomba[i]==7)
				{
					g.drawImage(explode[2], bposx[i], bposy[i], this);
					fimdejogo(bposx[i], bposy[i]);
					if (podeexplodir(CIMA, i))
					{
						g.drawImage(fogo[CIMA], bposx[i], bposy[i]-50, this);
						fimdejogo(bposx[i], bposy[i]-50);
					}
					if (podeexplodir(DIR, i))
					{
						g.drawImage(fogo[DIR], bposx[i]+50, bposy[i], this);
						fimdejogo(bposx[i]+50, bposy[i]);
					}
					if (podeexplodir(BAIXO, i))
					{
						g.drawImage(fogo[BAIXO], bposx[i], bposy[i]+50, this);
						fimdejogo(bposx[i], bposy[i]+50);
					}
					if (podeexplodir(ESQ, i))
					{
						g.drawImage(fogo[ESQ], bposx[i]-50, bposy[i], this);
						fimdejogo(bposx[i]-50, bposy[i]);
					}
				}
			}
			
		}
	}
  
	class Bomba extends Thread //classe que atualiza estado das bombas e dados sobre a mesma. o jogador só pode colocar 2 bombas ao mesmo tempo
	{
		public void run() 
		{
			while (true) 
			{	
				if (numbombas>0)
				{
					for (int i=0;i<2;i++)
					{
						if (bomba[i]==7)
						{
							bomba[i]++;
							numbombas--;
						}
						if (bomba[i]<7)
						{
							bomba[i]++;
							if (bomba[i]%2==0)
								bestado[i]=1;
							else
								bestado[i]=0;
						}									
					}
				}
				if (numbombasadv>0)
				{
					for (int i=2;i<3;i++)
					{
						if (bomba[i]==7)
						{
							bomba[i]++;
							numbombasadv--;
						}
						if (bomba[i]<7)
						{
							bomba[i]++;
							if (bomba[i]%2==0)
								bestado[i]=1;
							else
								bestado[i]=0;
						}									
					}
				}
				repaint();
				try 
				{
						sleep (180);
				} catch (InterruptedException ex) {} 
			}
		}
	}
	
	class Anda extends Thread //faz o jogador "andar" de uma posiçao para outra (a cada 50px)
	{
		public void run() 
		{
			while (true) 
			{	
				if (passo<50)
				{
					passo+=1;
					if (estado==0)
						estado=1;
					else
						estado=0;

					switch (paraonde) 
					{
						case  CIMA: posy--;
									break;
						case   DIR: posx++;
									break;
						case BAIXO: posy++;
									break;
						case   ESQ: posx--;
									break;
					}
					repaint();
				}
				try 
				{
						sleep (4);
				} catch (InterruptedException ex) {} 				
			}
		}
	}

	
	class Tecla extends KeyAdapter //verifica se o jogador apertou alguma tecla: ESC ou setas
	{
		public void keyPressed(KeyEvent e) 
		{
			if (e.getKeyCode()==KeyEvent.VK_ESCAPE) //se ESC, verifica se jogador deseja mesmo desistir
			{
				int resp = JOptionPane.showConfirmDialog(null,"Deseja mesmo desistir do jogo?", "ABANDONAR", JOptionPane.YES_NO_OPTION);
					if (resp == JOptionPane.YES_OPTION) 
					{
						desistiu = true;
						if (display.getText().equals("ESPERANDO ADVERSARIO..."))
							System.exit(0);
						return;
						//System.exit(0);
					}
			}
			if (display.getText().equals("ESPERANDO ADVERSARIO...")) //se o jogo ainda não começou, jogador 1 espera!
				return;
			if (passo<50)
				return;
			switch (e.getKeyCode()) //altera valores quando alguma das setas é apertada
			{
				case KeyEvent.VK_RIGHT: if (podeandar(DIR))
										{
											passo = 0;
											paraonde = DIR;
										}
										break;
				case KeyEvent.VK_LEFT:  if (podeandar(ESQ))
										{
											passo = 0;
											paraonde = ESQ;
										}
										break;
				case KeyEvent.VK_UP: 	if (podeandar(CIMA))
										{
											passo = 0;
											paraonde = CIMA;
										}
										break;
				case KeyEvent.VK_DOWN:  if (podeandar(BAIXO))
										{
											passo = 0;
											paraonde = BAIXO;
										}
										break;
				case KeyEvent.VK_SPACE: if (numbombas<2)
										{
											if (numbombas==1 && bomba[0]<7)
											{
												bomba[1]=0;
												bposx[1]=posx;
												bposy[1]=posy;
											}
											else
											{
											bomba[0]=0;
											bposx[0]=posx;
											bposy[0]=posy;
											}
											numbombas++;
										}
										break;
			}
		}
	}

	Cliente() //construtor
	{
		super("BOMBERWOMAN");
		lbl = new JLabel("<html>Pressione <font color='red'>ESC</font> para desistir/sair.</html>");
		display = new JLabel(" ");
		display.setBounds(0,550,550,20);
		display.setOpaque(true);
		display.setBackground(Color.black);
		display.setForeground(Color.lightGray);
		lbl.setBounds(0,570,550,20);
		lbl.setOpaque(true);
		lbl.setBackground(Color.black);
		lbl.setForeground(Color.white);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		add(display);
		add(lbl);
		add(des);
		this.setIconImage(icone.getImage());
		addKeyListener(new Tecla());
		new Anda().start();
		new Bomba().start();
		setSize(550,590);
		setUndecorated(true);
		setResizable(false);
		setLocationRelativeTo(null);
		setVisible(true);    	
		addMouseListener(new MouseAdapter() // PARA MOVER A JANELA USANDO UNDECORATED!
		{
			public void mousePressed(MouseEvent e) 
			{
				point.x = e.getX();
				point.y = e.getY();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() // PARA MOVER A JANELA USANDO UNDECORATED!
		{
			public void mouseDragged(MouseEvent e) 
			{
				Point p = getLocation();
				setLocation(p.x + e.getX() - point.x, p.y + e.getY() - point.y);
			}
		});
	}

	public static void main(String[] args) 
	{
		Cliente b = new Cliente();
		b.start();
		try {
            while (true) {
                playSound("bomberman.wav");
                Thread.sleep(212000);
            }
        } catch (InterruptedException e) {
        }
	}
	
	public static synchronized void playSound(final String arq) {
        try {
            AudioInputStream ais = AudioSystem
                    .getAudioInputStream(new File(arq));
            Clip c = AudioSystem.getClip(AudioSystem.getMixerInfo()[1]);
            c.open(ais);
            c.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
